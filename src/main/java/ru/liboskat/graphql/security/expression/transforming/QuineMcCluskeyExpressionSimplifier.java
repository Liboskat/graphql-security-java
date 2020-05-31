package ru.liboskat.graphql.security.expression.transforming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liboskat.graphql.security.exceptions.InternalErrorException;
import ru.liboskat.graphql.security.storage.TokenExpression;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.token.Token;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.liboskat.graphql.security.storage.token.OperatorToken.*;

/**
 * Реализация интерфейса {@link ExpressionSimplifier}, используемая для минимизации {@link TokenExpression}
 * в обратной польской записи с помощью алгоритма Куайна-МакКласки
 */
public class QuineMcCluskeyExpressionSimplifier implements ExpressionSimplifier {
    private static final Logger logger = LoggerFactory.getLogger(QuineMcCluskeyExpressionSimplifier.class);

    private static final int MAX_VARIABLE_COUNT = 10;

    /**
     * Минимизирует {@link TokenExpression} в обратной польской записи, используя
     * алгоритм минимизации булевых выражений Куйана-МакКласки
     *
     * @param expression {@link TokenExpression} в обратной польской записи
     * @return минимизированное выражение {@link TokenExpression} или то же самое выражение, если число переменных
     * больше 10
     */
    @Override
    public TokenExpression simplify(TokenExpression expression) {
        logger.debug("Simplification of expression {} started", expression);

        //достаем все переменные из выражения
        List<ComparisonToken> variables = expression.getTokens().stream()
                .filter(ComparisonToken.class::isInstance)
                .map(ComparisonToken.class::cast)
                .distinct()
                .collect(Collectors.toList());

        //если число переменных > максимального, не минимизируем
        if (variables.size() > MAX_VARIABLE_COUNT) {
            logger.debug("Expression {} can't be simplified, number of variables is more than 10", expression);
            return expression;
        }

        //генерируем всевозможные комбинации значений переменных
        Bit[][] variableCombinations = generateVariableCombinations(variables.size());

        //получаем все минтермы, которые дают true для выражения
        List<Bit[]> trueMinterms = getTrueMinterms(variableCombinations, expression, variables);

        //получаем соотношение число единиц - список минтермов с указанным числом единиц
        Map<Integer, Set<Minterm>> step = getNumberOfOnesToMinterms(trueMinterms);

        //получаем все совмещения минтермов, которые больше не совмещаются с другими
        Set<Minterm> notMatchableMinterms = new HashSet<>();
        while (!step.isEmpty()) {
            //делаем итерации по совмещениям минтермов
            step = nextStep(step, notMatchableMinterms);
        }

        //получаем все необходимые простые импликанты
        Set<Minterm> essentialMinterms = getEssentialImplicants(trueMinterms.size(), notMatchableMinterms);

        //получаем выражение в обратной польской записи на основе найденных простых импликантов
        TokenExpression rpnExpression = buildRpnExpression(essentialMinterms, variables);

        logger.debug("Simplification of expression {} ended. Simplified expression {}", expression, rpnExpression);
        return rpnExpression;
    }

    /**
     * @param variablesCount число переменных
     * @return двумерный массив с всевозможными комбинациями значений переменных
     */
    private Bit[][] generateVariableCombinations(int variablesCount) {
        int rows = 1 << variablesCount;
        Bit[][] table = new Bit[rows][variablesCount];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < variablesCount; j++) {
                table[i][variablesCount - j - 1] = (1 << j & i) != 0 ? Bit.TRUE : Bit.FALSE;
            }
        }
        return table;
    }

    /**
     * Возвращает все минтермы, результат которых = 1
     *
     * @param variableCombinations двумерный массив с всевозможными комбинациями значений переменных
     * @param expression           выражение
     * @param variables            переменные
     * @return минтермы, дающие true для выражения
     */
    private List<Bit[]> getTrueMinterms(Bit[][] variableCombinations,
                                        TokenExpression expression,
                                        List<ComparisonToken> variables) {
        List<Bit[]> minTerms = new ArrayList<>();
        for (Bit[] row : variableCombinations) {
            if (solveRpnExpression(expression, variables, row)) {
                minTerms.add(row);
            }
        }
        return minTerms;
    }

    /**
     * Возвращает map - число единиц -> список минтермов с таким числом единиц
     *
     * @param minterms минтермы, дающие true для выражения
     * @return map с соотношением число единиц -> список минтермов с таким числом единиц
     */
    private Map<Integer, Set<Minterm>> getNumberOfOnesToMinterms(List<Bit[]> minterms) {
        Map<Integer, Set<Minterm>> numberOfOnesToMinTerms = new HashMap<>();
        for (int i = 0; i < minterms.size(); i++) {
            Minterm minTerm = new Minterm(i, minterms.get(i));
            numberOfOnesToMinTerms
                    .computeIfAbsent(minTerm.numberOfOnes, HashSet::new)
                    .add(minTerm);
        }
        return numberOfOnesToMinTerms;
    }

    /**
     * Комбинирует минтермы отличающиеся на 1, сохраняет некомбинируемые минтермы
     *
     * @param numberOfOnesToMinterms map, содержащая число единиц к списку минтермов с таким числом единиц
     * @param notMatchableMinterms   список минтермов, комбинация которых невозможна
     * @return map, содержащая число единиц к списку минтермов с таким числом единиц,
     * полученная после всех возможных комбинаций минтермов из numberOfOnesToMinterms
     */
    private Map<Integer, Set<Minterm>> nextStep(Map<Integer, Set<Minterm>> numberOfOnesToMinterms,
                                                Set<Minterm> notMatchableMinterms) {
        Map<Integer, Set<Minterm>> nextStep = new HashMap<>();
        //проходим по спискам минтермов в соответствии с числом единиц
        for (Entry<Integer, Set<Minterm>> entry : numberOfOnesToMinterms.entrySet()) {
            //получаем минтермы со значением единиц n и n+1
            Set<Minterm> minterms = entry.getValue();
            Set<Minterm> toCompare = numberOfOnesToMinterms.get(entry.getKey() + 1);
            //если существуют минтермы с значением единиц n+1, пытаемся комбинировать
            if (toCompare != null) {
                //находим всевозможные комбинации
                Set<Minterm> combinations = getAllPossibleCombinations(minterms, toCompare);
                //если комбинации есть, добавляем их в map для последующих комбинаций
                if (!combinations.isEmpty()) {
                    nextStep.put(entry.getKey(), combinations);
                }
            }
            //все минтермы, которые не удалось скомбинировать, добавляем в список notMatchableMinterms
            minterms.stream()
                    .filter(Minterm::isNotMatched)
                    .forEach(notMatchableMinterms::add);
        }
        return nextStep;
    }

    /**
     * Возвращает все простые импликанты из некомбинируемых минтермов
     *
     * @param numberOfInitialTrueMinterms число всех минтермов, дающих 1
     * @param notMatchableMinterms минтермы, которые больше нельзя скомбинировать с другими
     * @return все простые импликанты
     */
    private Set<Minterm> getEssentialImplicants(int numberOfInitialTrueMinterms,
                                                Set<Minterm> notMatchableMinterms) {
        Set<Minterm> essentialImplicants = new HashSet<>();
        List<Integer> notCrossedByEssentialImplicants = new ArrayList<>();
        /* для каждого начального минтерма смотрим количество пересечений с некомбинируемыми минтермами,
        если пересечение одно - некомбинируемый минтерм - простой импликант */
        IntStream.range(0, numberOfInitialTrueMinterms).forEach(initialMintermIndex ->
                addEssentialImplicantOrElseAddToNotCrossed(initialMintermIndex, notMatchableMinterms,
                        essentialImplicants, notCrossedByEssentialImplicants));
        if (!notCrossedByEssentialImplicants.isEmpty()) {
            return notMatchableMinterms;
        }
        return essentialImplicants;
    }

    /**
     * Возвращает выражение из всех простых импликантов
     *
     * @param essentialMinterms простые импликанты
     * @param variables         переменные
     * @return итоговое выражение
     */
    private TokenExpression buildRpnExpression(Set<Minterm> essentialMinterms, List<ComparisonToken> variables) {
        TokenExpression tokenExpression = new TokenExpression();
        //если выражение первое, добавляем минтерм без оператора ИЛИ
        essentialMinterms.stream().findFirst().ifPresent(term -> addMinterm(tokenExpression, variables, term));
        //если выражение первое, добавляем минтерм и оператор ИЛИ
        essentialMinterms.stream().skip(1).forEach(term -> {
            addMinterm(tokenExpression, variables, term);
            tokenExpression.addToken(OR);
        });
        return tokenExpression;
    }

    /**
     * Добавляет минтерм в результирующее выражение
     *
     * @param expression выражение
     * @param variables  переменные
     * @param minterm    минтерм
     */
    private void addMinterm(TokenExpression expression, List<ComparisonToken> variables, Minterm minterm) {
        List<Bit> bits = minterm.binaryRepresentation;
        int addedCount = 0;
        //проходим по всем переменным и их значениям
        for (int i = 0; i < variables.size(); i++) {
            Bit bit = bits.get(i);
            //если bit = any (то есть может содержать и true, и false), добавлять его не нужно
            if (bit == Bit.ANY) {
                continue;
            }
            //добавляем переменную
            addVariable(expression, bit.booleanRepresentation, variables.get(i));
            addedCount++;
            //если уже добавлено больше одной, добавляем И
            if (addedCount > 1) {
                expression.addToken(AND);
            }
        }
    }


    /**
     * Добавляет переменную в зависимости от ее значения
     *
     * @param expression результирующее выражение
     * @param value      значение переменной
     * @param variable   переменная
     */
    private void addVariable(TokenExpression expression, boolean value, ComparisonToken variable) {
        expression.addToken(variable);
        //если значение - false, добавляем отрицание
        if (!value) {
            expression.addToken(NOT);
        }
    }

    /**
     * Если есть, добавляет основной импликант для минтерма, иначе добавляет минтерм в список непокрытых импликантами
     * @param initialMintermIndex индекс минтерма
     * @param notMatchableMinterms некомбинируемые минтермы
     * @param essentialImplicants основные импликанты
     * @param notCrossedMinterms минтермы, непокрытые импликантами
     */
    private void addEssentialImplicantOrElseAddToNotCrossed(int initialMintermIndex, Set<Minterm> notMatchableMinterms,
                                                            Set<Minterm> essentialImplicants,
                                                            List<Integer> notCrossedMinterms) {
        Optional<Minterm> essentialImplicant = getMintermWithUniqueCross(initialMintermIndex, notMatchableMinterms);
        if (essentialImplicant.isPresent()) {
            essentialImplicants.add(essentialImplicant.get());
        } else {
            notCrossedMinterms.add(initialMintermIndex);
        }
    }

    /**
     * Возвращает простой импликант для минтерма
     *
     * @param indexOfInitialTrueMinterm индекс минтерма
     * @param notMatchableMinterms некомбинируемые минтермы
     * @return если число пересечений с некомбинируемыми минтермами = 1,
     * возвращаем {@link Optional} с импликантом - некомбинируемым минтермом,
     * иначе возвращаем пустой {@link Optional}
     */
    private Optional<Minterm> getMintermWithUniqueCross(int indexOfInitialTrueMinterm,
                                                        Set<Minterm> notMatchableMinterms) {
        int crosses = 0;
        Minterm lastCrossedMinterm = null;
        for (Minterm minterm : notMatchableMinterms) {
            if (minterm.matchedMinterms.contains(indexOfInitialTrueMinterm)) {
                crosses++;
                lastCrossedMinterm = minterm;
            }
            if (crosses > 1) {
                return Optional.empty();
            }
        }
        return crosses == 1 ? Optional.of(lastCrossedMinterm) : Optional.empty();
    }

    /**
     * Возвращает всевозможные комбинации для списков минтермов
     * (комбинация создается, если два минтерма отличаются на 1 бит)
     * @param first первый список минтермов
     * @param second второй список минтермов
     * @return всевозможные комбинации для списков минтермов
     */
    private Set<Minterm> getAllPossibleCombinations(Set<Minterm> first, Set<Minterm> second) {
        return first.stream()
                .flatMap(firstTerm -> second.stream()
                        .map(secondTerm -> new Combination(firstTerm, secondTerm)))
                .filter(Combination::isDifferentByOneBit)
                .map(Combination::combine)
                .collect(Collectors.toSet());
    }

    /**
     * Решает выражение с переданными значениями
     *
     * @param expression выражение
     * @param variables  переменные
     * @param values     значения переменных
     * @return выражение, решенное с переданными значениями переменных
     */
    private boolean solveRpnExpression(TokenExpression expression, List<ComparisonToken> variables, Bit[] values) {
        Deque<Boolean> stack = new LinkedList<>();
        for (Token token : expression.getTokens()) {
            if (token instanceof ComparisonToken) {
                int index = IntStream.range(0, variables.size())
                        .filter(i -> token.equals(variables.get(i)))
                        .findFirst()
                        .orElseThrow(InternalErrorException::new);
                stack.push(values[index].booleanRepresentation);
            } else if (token instanceof OperatorToken) {
                OperatorToken operatorToken = ((OperatorToken) token);
                if (operatorToken == NOT) {
                    boolean stackFirst = stack.pop();
                    stack.push(!stackFirst);
                }
                if (operatorToken == AND) {
                    boolean stackFirst = stack.pop();
                    boolean stackSecond = stack.pop();
                    stack.push(stackFirst && stackSecond);
                }
                if (operatorToken == OR) {
                    boolean stackFirst = stack.pop();
                    boolean stackSecond = stack.pop();
                    stack.push(stackFirst || stackSecond);
                }
            } else {
                throw new InternalErrorException("Illegal token");
            }
        }
        return stack.pop();
    }

    /**
     * Класс, хранящий информацию о минтермах или их комбинациях
     */
    private static class Minterm {
        /**
         * Список с индексами скомбинированных минтермов
         */
        private final Set<Integer> matchedMinterms;
        /**
         * Побитовое представление минтерма
         */
        private final List<Bit> binaryRepresentation;
        /**
         * Скомбинировался ли минтерм с другим минтермом
         */
        private boolean notMatched;
        /**
         * Число единиц в двоичном представлении
         */
        private final int numberOfOnes;

        Minterm(int initialMintermIndex, Bit[] binaryRepresentation) {
            this(Collections.singleton(initialMintermIndex), Arrays.asList(binaryRepresentation));
        }

        Minterm(Set<Integer> matchedMinterms, List<Bit> binaryRepresentation) {
            this.matchedMinterms = matchedMinterms;
            this.binaryRepresentation = binaryRepresentation;
            this.notMatched = true;
            this.numberOfOnes = (int) binaryRepresentation.stream().filter(Bit.TRUE::equals).count();
        }

        boolean isNotMatched() {
            return notMatched;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Minterm match = (Minterm) o;
            if (binaryRepresentation == match.binaryRepresentation) return true;
            if (binaryRepresentation == null) return false;
            return Objects.equals(binaryRepresentation, match.binaryRepresentation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(binaryRepresentation);
        }
    }

    /**
     * Класс, комбинирующий два минтерма в один
     */
    private static class Combination {
        private final Minterm first;
        private final Minterm second;

        Combination(Minterm first, Minterm second) {
            this.first = first;
            this.second = second;
        }

        /**
         * @return отличаются ли два минтерма на один бит
         */
        boolean isDifferentByOneBit() {
            int differences = 0;
            for (int i = 0; i < first.binaryRepresentation.size() && differences < 2; i++) {
                if (first.binaryRepresentation.get(i) != second.binaryRepresentation.get(i)) {
                    differences++;
                }
            }
            return differences == 1;
        }

        /**
         * @return комбинация двух минтермов в один
         */
        Minterm combine() {
            //объединяем множества комбинированных минтермов
            Set<Integer> matchedMinTerms = new HashSet<>();
            matchedMinTerms.addAll(first.matchedMinterms);
            matchedMinTerms.addAll(second.matchedMinterms);
            //объединяем булевое представление
            List<Bit> representation = combineBooleanRepresentation(
                    first.binaryRepresentation, second.binaryRepresentation);
            //помечаем комбинируемые минтермы как скомбинированные
            first.notMatched = false;
            second.notMatched = false;
            //возвращаем новый минтерм на основе комбинации
            return new Minterm(matchedMinTerms, representation);
        }

        /**
         * Возвращает комбинацию двух булевых представлений,
         * если два бита отличаются, то ставится бит ANY
         * @param first первое булевое представление
         * @param second второе булевое представление
         * @return комбинация двух булевых представлений
         */
        private List<Bit> combineBooleanRepresentation(List<Bit> first, List<Bit> second) {
            List<Bit> combination = new ArrayList<>();
            for (int i = 0; i < first.size(); i++) {
                if (first.get(i) != second.get(i)) {
                    combination.add(Bit.ANY);
                } else {
                    combination.add(first.get(i));
                }
            }
            return combination;
        }
    }

    /**
     * Enum, используемый для обозначения 1, 0 и бита ANY (-)
     */
    private enum Bit {
        TRUE(true),
        FALSE(false),
        ANY;

        /**
         * Представление Bit в boolean
         */
        Boolean booleanRepresentation;

        Bit() {
        }

        Bit(Boolean booleanRepresentation) {
            this.booleanRepresentation = booleanRepresentation;
        }
    }
}

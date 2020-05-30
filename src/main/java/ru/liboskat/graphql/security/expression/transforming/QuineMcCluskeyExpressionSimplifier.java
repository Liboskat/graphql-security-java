package ru.liboskat.graphql.security.expression.transforming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liboskat.graphql.security.exceptions.InternalErrorException;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.token.Token;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * @param expression {@link TokenExpression} в обратной польской записи
     * @return минимизированное выражение {@link TokenExpression} или то же самое выражение, если число переменных
     * больше 10
     */
    @Override
    public TokenExpression simplify(TokenExpression expression) {
        logger.debug("Simplification of expression {} started", expression);

        List<ComparisonToken> variables = expression.getTokens().stream()
                .filter(ComparisonToken.class::isInstance)
                .map(ComparisonToken.class::cast)
                .distinct()
                .collect(Collectors.toList());

        if (variables.size() > MAX_VARIABLE_COUNT) {
            logger.debug("Expression {} can't be simplified, number of variables is more than 10", expression);
            return expression;
        }

        List<boolean[]> minTerms = getMinTerms(expression, variables);

        Map<Integer, Set<MinTermMatch>> step = getNumberOfOnesToMinTerms(minTerms);
        List<MinTermMatch> notMatchableMinTermMatches = new LinkedList<>();
        while (!step.isEmpty()) {
            step = nextStep(step, notMatchableMinTermMatches, variables.size());
        }

        TokenExpression rpnExpression = buildRpnExpression(
                getEssentialTerms(minTerms.size(), notMatchableMinTermMatches), variables);
        logger.debug("Simplification of expression {} ended. Simplified expression {}", expression, rpnExpression);
        return rpnExpression;
    }

    private List<boolean[]> getMinTerms(TokenExpression expression,
                                        List<ComparisonToken> variables) {
        boolean[][] table = generateTable(variables.size());
        List<boolean[]> minTerms = new ArrayList<>();
        for (boolean[] row : table) {
            if (solveRpnExpression(expression, variables, row)) {
                minTerms.add(row);
            }
        }
        return minTerms;
    }

    private boolean[][] generateTable(int variablesCount) {
        int rows = (int) Math.pow(2, variablesCount);
        boolean[][] table = new boolean[rows][variablesCount];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < variablesCount; j++) {
                table[i][variablesCount - j - 1] = (1 << j & i) != 0;
            }
        }
        return table;
    }

    private boolean solveRpnExpression(TokenExpression expression, List<ComparisonToken> variables, boolean[] values) {
        LinkedList<Boolean> stack = new LinkedList<>();
        for (Token token : expression.getTokens()) {
            if (token instanceof ComparisonToken) {
                int index = IntStream.range(0, variables.size())
                        .filter(i -> token.equals(variables.get(i)))
                        .findFirst()
                        .orElseThrow(InternalErrorException::new);
                stack.push(values[index]);
            } else if (token instanceof OperatorToken) {
                OperatorToken operatorToken = ((OperatorToken) token);
                if (operatorToken == OperatorToken.NOT) {
                    boolean stackFirst = stack.pop();
                    stack.push(!stackFirst);
                }
                if (operatorToken == OperatorToken.AND) {
                    boolean stackFirst = stack.pop();
                    boolean stackSecond = stack.pop();
                    stack.push(stackFirst && stackSecond);
                }
                if (operatorToken == OperatorToken.OR) {
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

    private Map<Integer, Set<MinTermMatch>> getNumberOfOnesToMinTerms(List<boolean[]> minTerms) {
        Map<Integer, Set<MinTermMatch>> numberOfOnesToMinTerms = new HashMap<>();
        for (int i = 0; i < minTerms.size(); i++) {
            boolean[] row = minTerms.get(i);
            int numberOfOnes = 0;
            List<Integer> matchedMinTerms = new ArrayList<>();
            matchedMinTerms.add(i);
            List<Integer> binaryRepresentation = new ArrayList<>();
            for (boolean bit : row) {
                if (bit) {
                    numberOfOnes++;
                    binaryRepresentation.add(1);
                } else {
                    binaryRepresentation.add(0);
                }
            }
            numberOfOnesToMinTerms
                    .computeIfAbsent(numberOfOnes, HashSet::new)
                    .add(new MinTermMatch(matchedMinTerms, binaryRepresentation));
        }
        return numberOfOnesToMinTerms;
    }

    private Map<Integer, Set<MinTermMatch>> nextStep(Map<Integer, Set<MinTermMatch>> previousStep,
                                                     List<MinTermMatch> notMatchableMinTermMatches,
                                                     int variableSize) {
        Map<Integer, Set<MinTermMatch>> nextStep = new HashMap<>();
        Set<MinTermMatch> current = previousStep.computeIfAbsent(0, HashSet::new);
        boolean noNewMatches = true;
        for (int numberOfOnes = 0; numberOfOnes <= variableSize; numberOfOnes++) {
            Set<MinTermMatch> next = previousStep.computeIfAbsent(numberOfOnes + 1, HashSet::new);
            Set<MinTermMatch> newMatches = new HashSet<>();
            current.forEach(curTerm -> next.stream()
                    .filter(nextTerm ->
                            isDifferentByOne(curTerm.getBinaryRepresentation(), nextTerm.getBinaryRepresentation()))
                    .forEach(nextTerm -> {
                        List<Integer> matchedMinTerms = new ArrayList<>();
                        matchedMinTerms.addAll(curTerm.getMatchedMinTerms());
                        matchedMinTerms.addAll(nextTerm.getMatchedMinTerms());
                        List<Integer> representation = combineBooleanRepresentation(
                                curTerm.getBinaryRepresentation(), nextTerm.getBinaryRepresentation());
                        curTerm.match();
                        nextTerm.match();
                        newMatches.add(new MinTermMatch(matchedMinTerms, representation));
                    }));
            if (noNewMatches && !newMatches.isEmpty()) {
                noNewMatches = false;
            }
            nextStep.put(numberOfOnes, newMatches);
            notMatchableMinTermMatches.addAll(current.stream()
                    .filter(MinTermMatch::isNotMatched)
                    .collect(Collectors.toList()));
            current = next;
        }
        if (noNewMatches) {
            return Collections.emptyMap();
        }
        return nextStep;
    }

    private boolean isDifferentByOne(List<Integer> first, List<Integer> second) {
        if (first.size() != second.size()) {
            throw new InternalErrorException("Terms can't have different size");
        }
        int differences = 0;
        for (int i = 0; i < first.size() && differences < 2; i++) {
            if (first.get(i) == null || second.get(i) == null) {
                throw new InternalErrorException("Term bit can't be null");
            }
            if (!first.get(i).equals(second.get(i))) {
                differences++;
            }
        }
        return differences == 1;
    }

    private List<Integer> combineBooleanRepresentation(List<Integer> first, List<Integer> second) {
        if (first.size() != second.size()) {
            throw new InternalErrorException("Terms can't have different size");
        }
        List<Integer> combination = new ArrayList<>();
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i) == null || second.get(i) == null) {
                throw new InternalErrorException("Term bit can't be null");
            }
            if (!first.get(i).equals(second.get(i))) {
                combination.add(-1);
            } else {
                combination.add(first.get(i));
            }
        }
        return combination;
    }

    private Set<List<Integer>> getEssentialTerms(int numberOfMinTerms,
                                                  List<MinTermMatch> notMatchableMinTermMatches) {
        Set<List<Integer>> essentialTerms = new HashSet<>();
        IntStream.range(0, numberOfMinTerms).forEach(minTerm -> {
            int matches = 0;
            MinTermMatch essentialMinTermMatch = null;
            Iterator<MinTermMatch> iterator = notMatchableMinTermMatches.iterator();
            while (iterator.hasNext() && matches < 2) {
                MinTermMatch match = iterator.next();
                if (match.getMatchedMinTerms().contains(minTerm)) {
                    matches++;
                    essentialMinTermMatch = match;
                }
            }
            if (matches == 1) {
                essentialTerms.add(essentialMinTermMatch.getBinaryRepresentation());
            }
        });
        return essentialTerms;
    }

    private TokenExpression buildRpnExpression(Set<List<Integer>> essentialTerms, List<ComparisonToken> variables) {
        TokenExpression tokenExpression = new TokenExpression();
        essentialTerms.stream().findFirst().ifPresent(term -> addTerm(tokenExpression, variables, term));
        essentialTerms.stream().skip(1).forEach(term -> {
            addTerm(tokenExpression, variables, term);
            tokenExpression.addToken(OperatorToken.OR);
        });
        return tokenExpression;
    }

    private void addTerm(TokenExpression expression, List<ComparisonToken> variables, List<Integer> term) {
        int addedCount = 0;
        for (int variableIndex = 0; variableIndex < term.size(); variableIndex++) {
            boolean added = addVariable(expression, term.get(variableIndex), variables.get(variableIndex));
            if (added) {
                addedCount++;
                if (addedCount > 1) {
                    expression.addToken(OperatorToken.AND);
                }
            }
        }
    }

    private boolean addVariable(TokenExpression expression, int variableValue, ComparisonToken variable) {
        boolean added = false;
        if (variableValue == 0) {
            expression.addToken(variable);
            expression.addToken(OperatorToken.NOT);
            added = true;
        } else if (variableValue == 1) {
            expression.addToken(variable);
            added = true;
        }
        return added;
    }

    private static class MinTermMatch {
        private final List<Integer> matchedMinTerms;
        private final List<Integer> binaryRepresentation;
        private boolean notMatched;

        MinTermMatch(List<Integer> matchedMinTerms, List<Integer> binaryRepresentation) {
            this.matchedMinTerms = matchedMinTerms;
            this.binaryRepresentation = binaryRepresentation;
            this.notMatched = true;
        }

        private void match() {
            this.notMatched = false;
        }

        private List<Integer> getMatchedMinTerms() {
            return matchedMinTerms;
        }

        private List<Integer> getBinaryRepresentation() {
            return binaryRepresentation;
        }

        private boolean isNotMatched() {
            return notMatched;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MinTermMatch match = (MinTermMatch) o;
            if (binaryRepresentation == match.binaryRepresentation) return true;
            if (binaryRepresentation == null) return false;
            return Objects.equals(binaryRepresentation, match.binaryRepresentation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(binaryRepresentation);
        }
    }
}

package org.har01d.imovie.web.qsl;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum RsqlSearchOperation {
    EQUAL(RSQLOperators.EQUAL),
    NOT_EQUAL(RSQLOperators.NOT_EQUAL),
    GREATER_THAN(RSQLOperators.GREATER_THAN),
    GREATER_THAN_OR_EQUAL(RSQLOperators.GREATER_THAN_OR_EQUAL),
    LESS_THAN(RSQLOperators.LESS_THAN),
    LESS_THAN_OR_EQUAL(RSQLOperators.LESS_THAN_OR_EQUAL),
    EMPTY(new ComparisonOperator("=e=")),
    NOT_EMPTY(new ComparisonOperator("=n=")),
    MEMBER(new ComparisonOperator("=m=")),
    IN(RSQLOperators.IN),
    NOT_IN(RSQLOperators.NOT_IN);

    private ComparisonOperator operator;

    RsqlSearchOperation(ComparisonOperator operator) {
        this.operator = operator;
    }

    public static RsqlSearchOperation getSimpleOperator(ComparisonOperator operator) {
        for (RsqlSearchOperation operation : values()) {
            if (operation.operator == operator) {
                return operation;
            }
        }
        throw new IllegalArgumentException("Invalid operator.");
    }

    public static Set<ComparisonOperator> operators() {
        return Arrays.stream(values()).map(e -> e.operator).collect(Collectors.toSet());
    }

}
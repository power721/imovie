package org.har01d.imovie.web.qsl;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public class GenericRsqlSpecification<T> implements Specification<T> {

    protected String property;
    protected ComparisonOperator operator;
    protected List<String> arguments;

    public GenericRsqlSpecification(
        String property, ComparisonOperator operator, List<String> arguments) {
        super();
        this.property = property;
        this.operator = operator;
        this.arguments = arguments;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        query.distinct(true);
        Collection<Object> args = new ArrayList<>();
        Path path = castArguments(root, args);
        Object argument = args.iterator().next();
        switch (RsqlSearchOperation.getSimpleOperator(operator)) {
            case EQUAL: {
                if (argument instanceof String) {
                    return builder.like(path, argument.toString().replace('*', '%'));
                } else if (argument == null) {
                    return builder.isNull(path);
                } else {
                    return builder.equal(path, argument);
                }
            }
            case NOT_EQUAL: {
                if (argument instanceof String) {
                    return builder.notLike(path, argument.toString().replace('*', '%'));
                } else if (argument == null) {
                    return builder.isNotNull(path);
                } else {
                    return builder.notEqual(path, argument);
                }
            }
            case GREATER_THAN: {
                return builder.greaterThan(path, argument.toString());
            }
            case GREATER_THAN_OR_EQUAL: {
                return builder.greaterThanOrEqualTo(path, argument.toString());
            }
            case LESS_THAN: {
                return builder.lessThan(path, argument.toString());
            }
            case LESS_THAN_OR_EQUAL: {
                return builder.lessThanOrEqualTo(path, argument.toString());
            }
            case IN:
                return path.in(args);
            case NOT_IN:
                return builder.not(path.in(args));
            case EMPTY:
                return builder.isEmpty(path);
            case NOT_EMPTY:
                return builder.isNotEmpty(path);
        }

        return null;
    }

    private Path castArguments(Root<T> root, Collection<Object> args) {
        String[] properties = property.split("\\.");
        Path<?> path;
        if (properties.length > 1) {
            path = root.join(properties[0]);
            for (int i = 1; i < properties.length; ++i) {
                path = path.get(properties[i]);
            }
        } else {
            path = root.get(property);
        }

        Class<?> type = path.getJavaType();

        for (String argument : arguments) {
            if (type.equals(Integer.class)) {
                try {
                    args.add(Integer.parseInt(argument));
                } catch (NumberFormatException e) {
                    args.add(null);
                }
            } else if (type.equals(Long.class)) {
                try {
                    args.add(Long.parseLong(argument));
                } catch (NumberFormatException e) {
                    args.add(null);
                }
            } else {
                args.add(argument);
            }
        }

        return path;
    }

}

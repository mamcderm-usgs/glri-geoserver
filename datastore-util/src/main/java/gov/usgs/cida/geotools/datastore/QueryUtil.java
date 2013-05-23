package gov.usgs.cida.geotools.datastore;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 *
 * @author tkunicki
 */
public class QueryUtil {
  
    public static boolean requiresAttributes(Query query, Collection<String> attributeNames) {
        if (query == null) {
            return true;
        }
        if (query == Query.ALL) {
            return true;
        }
        List<String> queryPropertyNames = Arrays.asList(query.getPropertyNames());
        List<String> filterPropertyNames = Arrays.asList(DataUtilities.attributeNames(query.getFilter()));
        return  !Collections.disjoint(queryPropertyNames, attributeNames) ||
                !Collections.disjoint(filterPropertyNames, attributeNames);
    }
    
    public static Object extractValueFromQueryFilter(Query query, AttributeDescriptor attributeDescriptor) {
        return extractValueFromQueryFilter(
                query,
                attributeDescriptor.getLocalName(),
                attributeDescriptor.getType().getBinding());
    }
    
    public static <T> T extractValueFromQueryFilter(Query query, String propertyName, Class<T> propertyClass) {
        if (query == null) {
            return null;
        }
        Filter filter = query.getFilter();
        if (filter == null) {
            return null;
        }
        PropertyEqualToVisitor<T> extractor = new PropertyEqualToVisitor<T>(propertyName, propertyClass);
        filter.accept(extractor, null);
        return extractor.getValue();
    }
    
    private static class PropertyEqualToVisitor<T> extends DefaultFilterVisitor {
    
        private final String propertyName;
        private final Class<T> propertyClass;

        private T value = null;

        public PropertyEqualToVisitor(String propertyName, Class<T> propertyClass) {
            this.propertyName = propertyName;
            this.propertyClass = propertyClass;
        }

        @Override
        public Object visit(PropertyIsEqualTo filter, Object data) {
            if (value == null) {
                Expression e1 = filter.getExpression1();
                Expression e2 = filter.getExpression2();
                value = attemptExtract(e1, e2);
                if (value == null) {
                    attemptExtract(e2, e1);
                }
            }
            return null;
        }

        private T attemptExtract(Expression e1, Expression e2) {
            if (e1 instanceof PropertyName && e2 instanceof Literal) {
                PropertyName pn1 = (PropertyName) e1;
                if (propertyName.equals(pn1.getPropertyName())) {
                    Literal l1 = (Literal) e2;
                    Object o = l1.getValue();
                    if (propertyClass.isInstance(o)) {
                        return propertyClass.cast(o);
                    }
                }
            }
            return null;
        }

        public T getValue() {
            return value;
        }
    }
}

package gov.usgs.cida.geotools.datastore;

import java.util.Date;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.joda.time.DateTime;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationTimeStampFilterExtractor extends DefaultFilterVisitor {
    
    private DateTime date = null;

    @Override
    public Object visit(PropertyIsEqualTo filter, Object data) {
        if (date == null) {
            Expression e1 = filter.getExpression1();
            Expression e2 = filter.getExpression2();
            date = attemptExtract(e1, e2);
            if (date == null) {
                attemptExtract(e2, e1);
            }
        }
        return null;
    }

    private DateTime attemptExtract(Expression e1, Expression e2) {
        if (e1 instanceof PropertyName && e2 instanceof Literal) {
            PropertyName pn1 = (PropertyName) e1;
            if ("timestamp".equals(pn1.getPropertyName())) {
                Literal l1 = (Literal) e2;
                Object o = l1.getValue();
                if (o instanceof Date) {
                    return new DateTime(o);
                }
            }
        }
        return null;
    }

    public DateTime getTimeStamp() {
        return date;
    }
    
}

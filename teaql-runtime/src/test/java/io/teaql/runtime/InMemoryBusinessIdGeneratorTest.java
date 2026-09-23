package io.teaql.runtime;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.businessid.BusinessIdErrorCode;
import io.teaql.core.businessid.BusinessIdException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

public class InMemoryBusinessIdGeneratorTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 9, 20);

    private InMemoryBusinessIdGenerator generator;
    private EntityDescriptor entityDesc;
    private PropertyDescriptor propDesc;
    private UserContext dummyContext;
    private Entity dummyEntity;

    @Before
    public void setUp() {
        generator = new InMemoryBusinessIdGenerator();
        entityDesc = new EntityDescriptor();
        entityDesc.setType("Order");
        
        propDesc = new PropertyDescriptor("orderNumber", new SimplePropertyType(String.class));
        
        DefaultUserContext context = new DefaultUserContext(
                TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).build());
        context.putAttribute(BusinessClock.class.getName(),
                (BusinessClock) ignored -> FIXED_DATE);
        dummyContext = context;
        dummyEntity = null;
    }

    @Test
    public void testGenerateBusinessId_Success() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 6");

        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);

        String dateStr = "20260920";
        
        Assert.assertEquals("ORD" + dateStr + "000001", id1);
        Assert.assertEquals("ORD" + dateStr + "000002", id2);
    }

    @Test
    public void testGenerateBusinessId_DefaultLength() {
        // No length specified, should default to 6
        propDesc.getAdditionalInfo().put("business_id_rule", "LOG");

        String id = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        String dateStr = "20260920";
        
        Assert.assertEquals("LOG" + dateStr + "000001", id);
    }

    @Test
    public void testGenerateBusinessId_DifferentPrefixes() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 4");
        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        
        PropertyDescriptor anotherProp = new PropertyDescriptor("logisticsNumber", new SimplePropertyType(String.class));
        anotherProp.getAdditionalInfo().put("business_id_rule", "LOG, 4");
        
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, anotherProp);
        
        String dateStr = "20260920";
        
        Assert.assertEquals("ORD" + dateStr + "0001", id1);
        Assert.assertEquals("LOG" + dateStr + "0001", id2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateBusinessId_MissingRule() {
        // No rule configured
        generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
    }

    @Test
    public void testContextDateControlsReset() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 2");
        Assert.assertEquals("ORD2026092001",
                generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc));
        dummyContext.putAttribute(BusinessClock.class.getName(),
                (BusinessClock) ignored -> LocalDate.of(2026, 9, 21));
        Assert.assertEquals("ORD2026092101",
                generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc));
    }

    @Test
    public void testRangeExhaustionDoesNotOverflowDigits() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 1");
        for (int i = 1; i <= 9; i++) {
            Assert.assertEquals("ORD20260920" + i,
                    generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc));
        }
        BusinessIdException failure = Assert.assertThrows(BusinessIdException.class,
                () -> generator.generateBusinessId(
                        dummyContext, dummyEntity, entityDesc, propDesc));
        Assert.assertEquals(BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                failure.getCode());
    }
}

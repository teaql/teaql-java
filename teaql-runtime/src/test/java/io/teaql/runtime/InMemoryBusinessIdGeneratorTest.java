package io.teaql.runtime;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimplePropertyType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class InMemoryBusinessIdGeneratorTest {

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
        
        dummyContext = null;
        dummyEntity = null;
    }

    @Test
    public void testGenerateBusinessId_Success() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 6");

        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        Assert.assertEquals("ORD" + dateStr + "000001", id1);
        Assert.assertEquals("ORD" + dateStr + "000002", id2);
    }

    @Test
    public void testGenerateBusinessId_DefaultLength() {
        // No length specified, should default to 6
        propDesc.getAdditionalInfo().put("business_id_rule", "LOG");

        String id = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        Assert.assertEquals("LOG" + dateStr + "000001", id);
    }

    @Test
    public void testGenerateBusinessId_DifferentPrefixes() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 4");
        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        
        PropertyDescriptor anotherProp = new PropertyDescriptor("logisticsNumber", new SimplePropertyType(String.class));
        anotherProp.getAdditionalInfo().put("business_id_rule", "LOG, 4");
        
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, anotherProp);
        
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        Assert.assertEquals("ORD" + dateStr + "0001", id1);
        Assert.assertEquals("LOG" + dateStr + "0001", id2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateBusinessId_MissingRule() {
        // No rule configured
        generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
    }
}

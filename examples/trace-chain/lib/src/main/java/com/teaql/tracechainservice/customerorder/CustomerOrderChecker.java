
package com.teaql.tracechainservice.customerorder;

import com.teaql.tracechainservice.orderitem.OrderItem;
import com.teaql.tracechainservice.orderitem.OrderItemChecker;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.payment.PaymentChecker;
import com.teaql.tracechainservice.platform.Platform;
import com.teaql.tracechainservice.platform.PlatformChecker;
import com.teaql.tracechainservice.shipment.Shipment;
import com.teaql.tracechainservice.shipment.ShipmentChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class CustomerOrderChecker implements Checker<CustomerOrder>{

    public String type(){
        return CustomerOrder.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
        if(needCheck(_context, customerOrder)){
            markAsChecked(_context, customerOrder);
            doCheck(_context, customerOrder, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
      if((customerOrder == null)){
         return;
      }
      if(customerOrder.newItem()){
      }else if(customerOrder.updateItem()){
        if(!customerOrder.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("orderNumber")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("description")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "description"), "Mutation requires a fully loaded entity");
        }

      }
      checkPlatform(_context, customerOrder.getProperty(CustomerOrder.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkOrderNumber(_context, customerOrder.getProperty(CustomerOrder.ORDER_NUMBER_PROPERTY), newLocation(_parentLocation, "order_number"));
      checkDescription(_context, customerOrder.getProperty(CustomerOrder.DESCRIPTION_PROPERTY), newLocation(_parentLocation, "description"));
      for(int i = 0; customerOrder.getOrderItemList() != null && i < customerOrder.getOrderItemList().size(); i++){
         OrderItem orderItem = customerOrder.getOrderItemList().get(i);
         new OrderItemChecker().checkAndFix(_context, orderItem, newLocation(_parentLocation, "order_item_list", i));
      }
      for(int i = 0; customerOrder.getPaymentList() != null && i < customerOrder.getPaymentList().size(); i++){
         Payment payment = customerOrder.getPaymentList().get(i);
         new PaymentChecker().checkAndFix(_context, payment, newLocation(_parentLocation, "payment_list", i));
      }
      for(int i = 0; customerOrder.getShipmentList() != null && i < customerOrder.getShipmentList().size(); i++){
         Shipment shipment = customerOrder.getShipmentList().get(i);
         new ShipmentChecker().checkAndFix(_context, shipment, newLocation(_parentLocation, "shipment_list", i));
      }
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkOrderNumber(UserContext _context, String orderNumber, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, orderNumber);
    if((orderNumber == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, orderNumber);

    }
    public void checkDescription(UserContext _context, String description, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, description);
    if((description == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, description);

    }
}
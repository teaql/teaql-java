
package com.teaql.tracechainservice.payment;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderChecker;
import com.teaql.tracechainservice.paymentattempt.PaymentAttempt;
import com.teaql.tracechainservice.paymentattempt.PaymentAttemptChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class PaymentChecker implements Checker<Payment>{

    public String type(){
        return Payment.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Payment payment, ObjectLocation _parentLocation){
        if(needCheck(_context, payment)){
            markAsChecked(_context, payment);
            doCheck(_context, payment, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Payment payment, ObjectLocation _parentLocation){
      if((payment == null)){
         return;
      }
      if(payment.newItem()){
      }else if(payment.updateItem()){
        if(!payment.isPropertyLoaded("customerOrder")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer_order"), "Mutation requires a fully loaded entity");
        }
        if(!payment.isPropertyLoaded("referenceCode")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "reference_code"), "Mutation requires a fully loaded entity");
        }

      }
      checkCustomerOrder(_context, payment.getProperty(Payment.CUSTOMER_ORDER_PROPERTY), newLocation(_parentLocation, "customer_order"));
      checkReferenceCode(_context, payment.getProperty(Payment.REFERENCE_CODE_PROPERTY), newLocation(_parentLocation, "reference_code"));
      for(int i = 0; payment.getPaymentAttemptList() != null && i < payment.getPaymentAttemptList().size(); i++){
         PaymentAttempt paymentAttempt = payment.getPaymentAttemptList().get(i);
         new PaymentAttemptChecker().checkAndFix(_context, paymentAttempt, newLocation(_parentLocation, "payment_attempt_list", i));
      }
    }

    public void checkCustomerOrder(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customerOrder);
    if((customerOrder == null)){
        return;
    }
    new CustomerOrderChecker().checkAndFix(_context, customerOrder, _parentLocation);
    }
    public void checkReferenceCode(UserContext _context, String referenceCode, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, referenceCode);
    if((referenceCode == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, referenceCode);

    }
}
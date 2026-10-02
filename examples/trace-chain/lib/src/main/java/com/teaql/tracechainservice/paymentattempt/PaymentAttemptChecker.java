
package com.teaql.tracechainservice.paymentattempt;

import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.payment.PaymentChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class PaymentAttemptChecker implements Checker<PaymentAttempt>{

    public String type(){
        return PaymentAttempt.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, PaymentAttempt paymentAttempt, ObjectLocation _parentLocation){
        if(needCheck(_context, paymentAttempt)){
            markAsChecked(_context, paymentAttempt);
            doCheck(_context, paymentAttempt, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, PaymentAttempt paymentAttempt, ObjectLocation _parentLocation){
      if((paymentAttempt == null)){
         return;
      }
      if(paymentAttempt.newItem()){
      }else if(paymentAttempt.updateItem()){
        if(!paymentAttempt.isPropertyLoaded("payment")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "payment"), "Mutation requires a fully loaded entity");
        }
        if(!paymentAttempt.isPropertyLoaded("referenceCode")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "reference_code"), "Mutation requires a fully loaded entity");
        }

      }
      checkPayment(_context, paymentAttempt.getProperty(PaymentAttempt.PAYMENT_PROPERTY), newLocation(_parentLocation, "payment"));
      checkReferenceCode(_context, paymentAttempt.getProperty(PaymentAttempt.REFERENCE_CODE_PROPERTY), newLocation(_parentLocation, "reference_code"));
    }

    public void checkPayment(UserContext _context, Payment payment, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, payment);
    if((payment == null)){
        return;
    }
    new PaymentChecker().checkAndFix(_context, payment, _parentLocation);
    }
    public void checkReferenceCode(UserContext _context, String referenceCode, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, referenceCode);
    if((referenceCode == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, referenceCode);

    }
}
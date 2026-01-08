package com.example.payment.service;

import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.payment.entity.Orders;
import com.example.payment.repository.OrdersRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.annotation.PostConstruct;

@Service
public class OrderService {

	@Autowired
	private OrdersRepository ordersRepository;
	
	@Value("${razorpay.key.id}")
	private String razorpayId;
	
	@Value("${razorpay.key.secret}")
	private String razorpaySecret;
	
	
	private RazorpayClient razorpayCLient;
	
	@PostConstruct
	public void init() throws RazorpayException {
		this.razorpayCLient = new RazorpayClient(razorpayId, razorpaySecret);
	}
	
	public Orders createOrder(Orders order) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("amount", order.getAmount() * 100); // amount in paise
        options.put("currency", "INR");
        options.put("receipt", order.getEmail());
        Order razorpayOrder = razorpayCLient.orders.create(options);
        if(razorpayOrder != null) {
        order.setRazorpayOrderId(razorpayOrder.get("id"));
        order.setOrderStatus(razorpayOrder.get("status"));
        }
        return ordersRepository.save(order);
    }
	
	public Orders updateStatus(Map<String, String> map) {

	    String orderId = map.get("razorpay_order_id");
	    String paymentId = map.get("razorpay_payment_id");
	    String signature = map.get("razorpay_signature");

	    Orders order = ordersRepository.findByRazorpayOrderId(orderId);

	    // USER CANCELLED
	    if (paymentId == null || signature == null) {
	        order.setOrderStatus("PAYMENT CANCELLED");
	        return ordersRepository.save(order);
	    }

	    // VERIFY SIGNATURE
	    boolean isValid = verifySignature(orderId, paymentId, signature);

	    if (isValid) {
	        order.setOrderStatus("PAYMENT SUCCESS");
	    } else {
	        order.setOrderStatus("PAYMENT FAILED");
	    }

	    return ordersRepository.save(order);
	}
	
	private boolean verifySignature(String orderId, String paymentId, String signature) {
	    try {
	        String payload = orderId + "|" + paymentId;

	        Mac mac = Mac.getInstance("HmacSHA256");
	        SecretKeySpec secretKey =
	            new SecretKeySpec(razorpaySecret.getBytes(), "HmacSHA256");

	        mac.init(secretKey);
	        byte[] hash = mac.doFinal(payload.getBytes());

	        String generatedSignature =
	            Base64.getEncoder().encodeToString(hash);

	        return generatedSignature.equals(signature);

	    } catch (Exception e) {
	        return false;
	    }
	}


}

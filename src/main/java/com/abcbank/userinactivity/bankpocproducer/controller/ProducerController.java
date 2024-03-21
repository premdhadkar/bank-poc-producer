package com.abcbank.userinactivity.bankpocproducer.controller;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.abcbank.userinactivity.bankpocproducer.model.Customer;

@Controller
public class ProducerController {

	private final KafkaTemplate<Integer, Customer> kafkaTemplate;
	private static SendResult<Integer, Customer> sendResult = null;

	@Value("${topic.heartbeat}")
	private String heartbeatTopic;

	@Value("${topic.signup-history}")
	private String signupHistoryTopic;

	@Value("${user.heartbeat.interval.ms}")
	private long heartbeatIntervalInMillis;

	@Autowired
	public ProducerController(KafkaTemplate<Integer, Customer> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@PostMapping("/start-heart-beat")
	public ResponseEntity<Object> heartbeat(@RequestBody Customer customer) throws InterruptedException {
		new Thread(() -> {
			while (true) {
				try {
					sendResult = kafkaTemplate.send(heartbeatTopic, customer.getAccno(), customer).get();
					System.out.println("HeartBeat Sent; Acc No: " + customer.getAccno() + " Offset: "
							+ sendResult.getRecordMetadata().offset());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					Thread.sleep(heartbeatIntervalInMillis);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
		Thread.sleep(5000);
		if (sendResult != null)
			System.out.println("SendResult is not null");
		Customer response = Customer.builder().accno(customer.getAccno()).timestamp(Timestamp.from(Instant.now()))
				.description("Started Heartbeat").build();
		Thread.sleep(1000);
		return new ResponseEntity<Object>(response, HttpStatus.CREATED);
	}


	@PostMapping("/signup")
	public ResponseEntity<Object> signup(@RequestBody Customer customer) throws InterruptedException, ExecutionException    {
		SendResult<Integer, Customer> singupSendResult = kafkaTemplate.send(signupHistoryTopic, customer.getAccno(), customer).get();

		
		Customer response = Customer.builder().accno(customer.getAccno()).timestamp(Timestamp.from(Instant.now()))
				.description("User Created").build();
		if (singupSendResult != null)
			return new ResponseEntity<Object>(response, HttpStatus.CREATED);
		else
			return new ResponseEntity<Object>(new Exception("Unable to Create first Signup, due to producer error"), HttpStatus.NOT_IMPLEMENTED);
	}

}

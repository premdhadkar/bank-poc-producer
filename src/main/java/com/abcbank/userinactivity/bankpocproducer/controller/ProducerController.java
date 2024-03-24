package com.abcbank.userinactivity.bankpocproducer.controller;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.abcbank.userinactivity.bankpocproducer.model.Customer;
import com.abcbank.userinactivity.bankpocproducer.repository.CustomerRepository;

@Controller
public class ProducerController {

	private static SendResult<Integer, Customer> sendResult = null;

	@Value("${topic.heartbeat}")
	private String heartbeatTopic;

	@Value("${topic.signup-history}")
	private String signupHistoryTopic;

	@Value("${user.heartbeat.interval.ms}")
	private long heartbeatIntervalInMillis;

	@Autowired
	KafkaTemplate<Integer, Customer> kafkaTemplate;

	@Autowired
	CustomerRepository customerRepository;

	@PostMapping("/start-heart-beat")
	public ResponseEntity<Object> heartbeat(@RequestBody Customer customer) throws InterruptedException {
		new Thread(() -> {
			while (true) {
				try {
					customer.setDescription("heart-beat");
					customer.setTimestamp(Timestamp.from(Instant.now()));
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

	@PostMapping("/start-heart-beat-no-loop")
	public ResponseEntity<Object> heartbeatNoLoop(@RequestBody Customer customer) throws InterruptedException {
		try {
			customer.setDescription("heart-beat");
			customer.setTimestamp(Timestamp.from(Instant.now()));
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
		Customer response = Customer.builder().accno(customer.getAccno()).timestamp(Timestamp.from(Instant.now()))
				.description("Started Heartbeat").build();
		return new ResponseEntity<Object>(response, HttpStatus.CREATED);
	}

//	@PostMapping("/signup")
//	public ResponseEntity<Object> signup(@RequestBody Customer customer) throws InterruptedException, ExecutionException    {
//		Timestamp ts = Timestamp.from(Instant.now());
//		customer.setDescription("User Created");
//		customer.setTimestamp(ts);
//		SendResult<Integer, Customer> singupSendResult = kafkaTemplate.send(signupHistoryTopic, customer.getAccno(), customer).get();
//
//		
//		
//		if (singupSendResult != null)
//			return new ResponseEntity<Object>(customer, HttpStatus.CREATED);
//		else
//			return new ResponseEntity<Object>(new Exception("Unable to Create first Signup, due to producer error"), HttpStatus.NOT_IMPLEMENTED);
//	}

	@PostMapping("/signup")
	public ResponseEntity<Object> signup(@RequestBody Customer customer)
			throws InterruptedException, ExecutionException {
		Timestamp ts = Timestamp.from(Instant.now());
		customer.setDescription("User Created");
		customer.setTimestamp(ts);
		customer.setPassword(customer.getPassword() == null ? "1234" : customer.getPassword());
		Customer singupSendResult = customerRepository.save(customer);

		if (singupSendResult != null)
			return new ResponseEntity<Object>(customer, HttpStatus.CREATED);
		else
			return new ResponseEntity<Object>(new Exception("Unable to Create first Signup, due to database error"),
					HttpStatus.NOT_IMPLEMENTED);
	}

	@GetMapping("/getAllCustomers")
	public List<Customer> getAllCustomers() {
		List<Customer> result = customerRepository.findAll();
		return result;
	}
}

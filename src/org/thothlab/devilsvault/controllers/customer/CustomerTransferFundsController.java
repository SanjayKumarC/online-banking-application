package org.thothlab.devilsvault.controllers.customer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
//import org.thothlab.devilsvault.jdbccontrollers.customerdoa.CustomerDAOHelper;
//import org.thothlab.devilsvault.jdbccontrollers.customerdoa.ExternalTransactionDAO;
import org.thothlab.devilsvault.dao.transaction.TransferDAO;
import org.thothlab.devilsvault.dao.userauthentication.OtpDaoImpl;
import org.thothlab.devilsvault.dao.transaction.TransactionDaoImpl;
//import org.thothlab.devilsvault.db.model.ExternalUser;
import org.thothlab.devilsvault.db.model.Transaction;

@Controller
public class CustomerTransferFundsController {

	String role;
	int userID;
	String username;

	public void setGlobals(HttpServletRequest request) {
		role = (String) request.getSession().getAttribute("role");
		System.out.println(role + "role");
		userID = (int) request.getSession().getAttribute("userID");
		username = (String) request.getSession().getAttribute("username");
	}

	@RequestMapping("/customer/transferfunds")
	public ModelAndView initializeTransferFunds(HttpServletRequest request) throws IOException {
		ModelAndView model = new ModelAndView("customerPages/transferFunds");
		// model.addObject("displayPanel","Hello Gaurav");
		return model;
	}

	@RequestMapping("/customer/externalFundTransfer")
	public ModelAndView externalFundTransfer(HttpServletRequest request) throws IOException {
		setGlobals(request);
		ModelAndView model = new ModelAndView("customerPages/transferFunds");
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		TransferDAO transferDAO = ctx.getBean("transferDAO", TransferDAO.class);
		int payerId = userID;
		List<Integer> currentUserAccounts = transferDAO.getMultipleAccounts(payerId);
		List<String> populatedPayeeAccounts = new ArrayList<>();

		List<String> userAccounts = new ArrayList<>();
		for (Integer currentElements : currentUserAccounts) {
			transferDAO.getRelatedAccounts(currentElements, populatedPayeeAccounts);
			transferDAO.getPayerAccounts(currentElements, userAccounts);
		}

		for (String elem : populatedPayeeAccounts) {
			System.out.println("Payee: " + elem);
		}
		model.addObject("payeeAccounts", populatedPayeeAccounts);
		model.addObject("userAccounts", userAccounts);
		model.addObject("displayPanel", "externalFundTransfer");
		request.getSession().setAttribute("userAccounts", userAccounts);
		request.getSession().setAttribute("payeeAccounts", populatedPayeeAccounts);
		ctx.close();
		return model;
	}

	@RequestMapping("/customer/internalFundTransfer")
	public ModelAndView internalFundTransfer(HttpServletRequest request) throws IOException {
		setGlobals(request);
		ModelAndView model = new ModelAndView("customerPages/transferFunds");
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		TransferDAO transferDAO = ctx.getBean("transferDAO", TransferDAO.class);
		int payerId = userID;
		List<Integer> currentUserAccounts = transferDAO.getMultipleAccounts(payerId);

		List<String> userAccounts = new ArrayList<>();
		for (Integer currentElements : currentUserAccounts) {
			transferDAO.getPayerAccounts(currentElements, userAccounts);
		}

		model.addObject("userAccounts", userAccounts);
		model.addObject("displayPanel", "internalFundTransfer");
		request.getSession().setAttribute("userAccounts", userAccounts);
		ctx.close();
		return model;
	}

	@RequestMapping("/customer/emailPhoneFundTransfer")
	public ModelAndView emailPhoneFundTransfer(HttpServletRequest request) throws IOException {
		setGlobals(request);
		ModelAndView model = new ModelAndView("customerPages/transferFunds");
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		TransferDAO transferDAO = ctx.getBean("transferDAO", TransferDAO.class);
		int payerId = userID;
		List<Integer> currentUserAccounts = transferDAO.getMultipleAccounts(payerId);

		List<String> userAccounts = new ArrayList<>();
		for (Integer currentElements : currentUserAccounts) {
			transferDAO.getPayerAccounts(currentElements, userAccounts);
		}

		model.addObject("userAccounts", userAccounts);
		model.addObject("displayPanel", "emailPhoneFundTransfer");
		request.getSession().setAttribute("userAccounts", userAccounts);
		ctx.close();
		return model;
	}

	@RequestMapping(value = "/customer/ExternalTransferSubmit", method = RequestMethod.POST)
	public ModelAndView ExternalSubmit(HttpServletRequest request) throws ParseException {

		List<String> userAccounts = (List<String>) request.getSession().getAttribute("userAccounts");
		List<String> populatedPayeeAccounts = (List<String>) request.getSession().getAttribute("payeeAccounts");

		setGlobals(request);
		System.out.println("------------");
		// TransferDAO transferDAO = CustomerDAOHelper.transferDAO();
		ModelAndView model = new ModelAndView("customerPages/transferConfirmation");
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		TransferDAO transferDAO = ctx.getBean("transferDAO", TransferDAO.class);
		// System.out.println(request.getParameter("etpdatetimepicker_result"));
		String amount_str = request.getParameter("etpinputAmount");

		// Check if amount is valid Bigdecimal
		if (!(amount_str.replaceAll(",", "").matches("^(\\d+\\.)?\\d+$")) || amount_str.isEmpty()) {
			model.addObject("success", false);
			model.addObject("error_msg", "Invalid Amount!");
			ctx.close();
			return model;
		}

		// Check if payee account input is in the expected format
		String payeeAccount_str = request.getParameter("etpselectPayeeAccount");

		BigDecimal amount = new BigDecimal(amount_str);
		int payerAccountNumber = Integer.parseInt(request.getParameter("etpselectPayerAccount").split(":")[0]);
		String payerAccountType = request.getParameter("etpselectPayerAccount").split(":")[1];
		int payeeAccountNumber = -1;

		if (payeeAccount_str.contains(":")) {
			payeeAccountNumber = Integer.parseInt(request.getParameter("etpselectPayeeAccount").split(":")[1]);
			String payeeName = request.getParameter("etpselectPayeeAccount").split(":")[0];
			if (!(populatedPayeeAccounts.contains(payeeName + ":" + payeeAccountNumber)
					&& userAccounts.contains(payerAccountNumber + ":" + payerAccountType))) {
				model.addObject("success", false);
				model.addObject("error_msg", " Invalid Credentials!");
				ctx.close();
				return model;
			}
			model.addObject("payee_info", payeeName + "-" + payeeAccountNumber);
		} else {
			payeeAccountNumber = Integer.parseInt(request.getParameter("etpselectPayeeAccount"));
			boolean payeeAccountExists = transferDAO.checkAccountExists(payeeAccountNumber);
			System.out.println(payeeAccountExists);
			if (!payeeAccountExists) {
				model.addObject("success", false);
				model.addObject("error_msg", "Invalid payee account!");
				ctx.close();
				return model;
			}
		}
		String description = request.getParameter("etpTextArea");

		boolean amountValid = transferDAO.validateAmount(payerAccountNumber, amount);
		if (!amountValid) {
			model.addObject("success", false);
			model.addObject("error_msg", "Insufficient balance!");
			ctx.close();
			return model;
		}

		TransactionDaoImpl extTransactionDAO = ctx.getBean("TransactionDao", TransactionDaoImpl.class);

		Transaction extTransferTrans = extTransactionDAO.createExternalTransaction(payerAccountNumber, amount,
				payeeAccountNumber, description, "externalFundTfr");
		extTransactionDAO.save(extTransferTrans, "transaction_pending");
		transferDAO.updateHold(payerAccountNumber, amount);
		model.addObject("success", true);
		model.addObject("payer_info", payerAccountNumber + "-" + payerAccountType);
		model.addObject("Amount", amount);
		ctx.close();
		return model;
	
	}

	@RequestMapping(value = "/customer/InternalTransferSubmit", method = RequestMethod.POST)
	public ModelAndView InternalSubmit(HttpServletRequest request) throws ParseException {

		List<String> userAccounts = (List<String>) request.getSession().getAttribute("userAccounts");
		ModelAndView model = new ModelAndView("customerPages/transferConfirmation");
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		TransferDAO transferDAO = ctx.getBean("transferDAO", TransferDAO.class);

		setGlobals(request);
		System.out.println("------------");
		// TransferDAO transferDAO = CustomerDAOHelper.transferDAO();

		String amount_str = request.getParameter("itpinputAmount");
		// Check if amount is valid Bigdecimal
		if (!(amount_str.replaceAll(",", "").matches("^(\\d+\\.)?\\d+$")) || amount_str.isEmpty()) {
			model.addObject("success", false);
			model.addObject("error_msg", "Invalid Amount!");
			return model;
		}

		BigDecimal amount = new BigDecimal(amount_str);
		int payerAccountNumber = Integer.parseInt(request.getParameter("itpselectPayerAccount").split(":")[0]);
		String payerAccountType = request.getParameter("itpselectPayerAccount").split(":")[1];
		int payeeAccountNumber = Integer.parseInt(request.getParameter("itpselectPayeeAccount").split(":")[0]);
		String payeeAccountType = request.getParameter("itpselectPayeeAccount").split(":")[1];
		String description = request.getParameter("itpTextArea");

		if (!(userAccounts.contains(payerAccountNumber + ":" + payerAccountType)
				&& userAccounts.contains(payeeAccountNumber + ":" + payeeAccountType))) {
			model.addObject("success", false);
			model.addObject("error_msg", " Invalid Request!");
			ctx.close();
			return model;
		}

		if ((payerAccountNumber + ":" + payerAccountType)
				.equalsIgnoreCase(payeeAccountNumber + ":" + payeeAccountType)) {
			model.addObject("success", false);
			model.addObject("error_msg", " Payer and Payee account are same!");
			ctx.close();
			return model;

		}

		boolean amountValid = transferDAO.validateAmount(payerAccountNumber, amount);

		if (!amountValid) {

			System.out.println("Inadequate balance!");
			model.addObject("success", false);
			model.addObject("error_msg", "Insufficient balance!");
			ctx.close();
			return model;
		}
		TransactionDaoImpl extTransactionDAO = ctx.getBean("TransactionDao", TransactionDaoImpl.class);
		Transaction extTransferTrans = extTransactionDAO.createExternalTransaction(payerAccountNumber, amount,
				payeeAccountNumber, description, "internalFundTfr");
		extTransactionDAO.save(extTransferTrans, "transaction_pending");
		transferDAO.updateHold(payerAccountNumber, amount);
		model.addObject("success", true);
		model.addObject("payee_info", payeeAccountNumber + "-" + payeeAccountType);
		model.addObject("payer_info", payerAccountNumber + "-" + payerAccountType);
		model.addObject("Amount", amount);
		ctx.close();
		return model;
	}

	@RequestMapping(value = "/customer/EmailPhoneTransferSubmit", method = RequestMethod.POST)
	public ModelAndView EmailPhoneSubmit(HttpServletRequest request) throws ParseException {

		List<String> userAccounts = (List<String>) request.getSession().getAttribute("userAccounts");

		setGlobals(request);
		String modeOfTransfer = request.getParameter("eptpModeOfTransfer");
		String inputMode = request.getParameter("eptpinputMode");
		// TransferDAO transferDAO = CustomerDAOHelper.transferDAO();
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		TransferDAO transferDAO = ctx.getBean("transferDAO", TransferDAO.class);
		ModelAndView model = new ModelAndView("customerPages/transferConfirmation");
		int PayeeAccountNumber = transferDAO.fetchAccountNumber(modeOfTransfer, inputMode);
		int payerAccountNumber = Integer.parseInt(request.getParameter("eptpselectPayerAccount").split(":")[0]);
		String payerAccountType = request.getParameter("eptpselectPayerAccount").split(":")[1];

		if (!(userAccounts.contains(payerAccountNumber + ":" + payerAccountType))) {
			model.addObject("success", false);
			model.addObject("error_msg", " Invalid Request!");
			ctx.close();
			return model;
		}

		String amount_str = request.getParameter("eptpinputAmount");
		// Check if amount is valid Bigdecimal [0-9]*\\.?[0-9]+
		if (!(amount_str.replaceAll(",", "").matches("^(\\d+\\.)?\\d+$")) || amount_str.isEmpty()) {
			model.addObject("success", false);
			model.addObject("error_msg", "Invalid Amount!");
			ctx.close();
			return model;
		}
		BigDecimal amount = new BigDecimal(amount_str);
		String description = request.getParameter("eptpTextArea");

		if (PayeeAccountNumber != -1) {
			boolean amountValid = transferDAO.validateAmount(payerAccountNumber, amount);
			if (!amountValid) {
				System.out.println("Inadequate balance!");
				model.addObject("success", false);
				model.addObject("error_msg", "Insufficient balance!");
				ctx.close();
				return model;
			}
			
			TransactionDaoImpl extTransactionDAO = ctx.getBean("TransactionDao", TransactionDaoImpl.class);
			Transaction extTransferTrans = extTransactionDAO.createExternalTransaction(payerAccountNumber, amount,
					PayeeAccountNumber, description, "emailphoneFundTfr");
			
			BigDecimal lessThan1k = new BigDecimal(1000);
			if(extTransferTrans.getAmount().compareTo(lessThan1k) == -1) {
				extTransactionDAO.save(extTransferTrans, "transaction_pending");
				transferDAO.updateHold(payerAccountNumber, amount);
				model.addObject("success", true);
				model.addObject("payee_info", inputMode);
				model.addObject("payer_info", payerAccountNumber + "-" + payerAccountType);
				model.addObject("Amount", amount);
				ctx.close();
				return model;
			}
			
			model = new ModelAndView("customerPages/transferOTP");
			OtpDaoImpl otpdao = ctx.getBean("OtpDaoImpl", OtpDaoImpl.class);
			String email = username;
			String message = otpdao.processOTP(email);
			
			if(message.equals("Account Locked")) {
				model = new ModelAndView("customerPages/transferConfirmation");
				model.addObject("success", false);
				model.addObject("error_msg", "OTP Locked");
			} else {
				request.getSession().setAttribute("transaction", extTransferTrans);
				request.getSession().setAttribute("inputMode",inputMode); 
				request.getSession().setAttribute("eptpselectPayerAccount",payerAccountType);
			}
			ctx.close();
			return model;
		} else {
			model.addObject("success", false);
			model.addObject("error_msg", "Payee account not found for given Email/Phone!");
			ctx.close();
			return model;
		}

	}
	
	@RequestMapping(value = "/customer/verifyTransactionOTP", method = RequestMethod.POST)
	public ModelAndView verifyTransactionOTP(HttpServletRequest request, @RequestParam("otpdata") String otp) throws ParseException {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		setGlobals(request);
		TransactionDaoImpl extTransactionDAO = ctx.getBean("TransactionDao", TransactionDaoImpl.class);
		TransferDAO transferDAO = ctx.getBean("transferDAO", TransferDAO.class);
		OtpDaoImpl otpdao = ctx.getBean("OtpDaoImpl",OtpDaoImpl.class);
		Transaction extTransferTrans = (Transaction)request.getSession().getAttribute("transaction");
		String email = username;
		String message = otpdao.verifyOTP(otp, email);
		if(message.equals("OTP Validated")) {
			ModelAndView model = new ModelAndView("customerPages/transferConfirmation");
			extTransactionDAO.save(extTransferTrans, "extTransferTrans");
			transferDAO.updateHold(extTransferTrans.getPayer_id(), extTransferTrans.getAmount());
			model.addObject("success", true);
			String inputMode = (String)request.getSession().getAttribute("inputMode");
			model.addObject("payee_info", inputMode);
			String payerAccountType = (String)request.getSession().getAttribute("eptpselectPayerAccount");
			model.addObject("payer_info", extTransferTrans.getPayer_id() + "-" + payerAccountType);
			model.addObject("Amount", extTransferTrans.getAmount());
			ctx.close();
			return model;
		}
		else {
			ModelAndView model = new ModelAndView("customerPages/transferOTP");
			model.addObject("error_msg", message);
			ctx.close();
			return model;
		}
		
	}

}
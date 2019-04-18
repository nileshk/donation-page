package com.nileshk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class LoggingController {

	private static final Logger logger = LogManager.getLogger(LoggingController.class);

	@RequestMapping(value = "/log", method = POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ResponseBody
	public void log(@RequestBody String logString, HttpServletRequest request) {
		String idPrefix = "";
		if (request != null && request.getSession() != null) {
			idPrefix = request.getSession().getId() + ": ";
		}
		logger.info(idPrefix + logString);
	}
}

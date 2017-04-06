package com.nileshk;

import com.stripe.model.Charge;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface Processors {

	@Async
	void postProcess(Map<String, Object> param, Charge charge);

}

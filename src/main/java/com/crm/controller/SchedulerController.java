package com.crm.controller;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.crm.exception.CustomException;
import com.crm.model.Deal;
import com.crm.model.Leads;
import com.crm.model.PaymentMilestone;
import com.crm.repository.DealsRepo;
import com.crm.repository.LeadsRepo;
import com.crm.repository.PaymentMilestoneRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskCanceledRequest;

@Component
public class SchedulerController {

    @Autowired
    private PaymentMilestoneRepo paymentMilestoneRepo;

    @Autowired
    DealsRepo dealsRepo;

//    @Scheduled(cron = "0 0 0 * * *") 		// Runs at midnight every day
    public ResponseEntity<?> scheduleTask() {

        try {
            List<PaymentMilestone> allMilestones = paymentMilestoneRepo.findAll();
            LocalDate currentDate = LocalDateTime.now().toLocalDate();
            for(PaymentMilestone pm: allMilestones) {
                LocalDate startDate = pm.getDeliveryDate().toLocalDate();
                LocalDate endDate = startDate.plusDays(pm.getExpectedEndDate());
//                long days = ChronoUnit.DAYS.between(currentDate, startDate);
                if(endDate.isAfter(currentDate)) {
                    Deal deal = new Deal();
                    deal.setPaymentMilestone(pm);
                    dealsRepo.save(deal);
                }
            }

            return ResponseEntity.ok("Milestone converted to deal !!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
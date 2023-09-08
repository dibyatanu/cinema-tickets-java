package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;
import java.util.function.Predicate;

public class TicketServiceImpl implements TicketService {
    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(final TicketPaymentService ticketPaymentService,final SeatReservationService seatReservationService){
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }


    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)  {
        validatePurchaseRequest(accountId, ticketTypeRequests);

       var totalAmountToPay= Arrays.stream(ticketTypeRequests)
                .mapToInt(ticketTypeRequest -> ticketTypeRequest.noOfTickets() * lookUpTicketPrice(ticketTypeRequest.type()))
                .sum();
        ticketPaymentService.makePayment(accountId,totalAmountToPay);

       var totalSeatsToAllocate= Arrays.stream(ticketTypeRequests)
               .filter( ticketTypeRequest -> !ticketTypeRequest.type().equals(TicketTypeRequest.Type.INFANT))
               .mapToInt(TicketTypeRequest::noOfTickets)
               .sum();
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
    }

    private void validatePurchaseRequest(final Long accountId,final TicketTypeRequest... ticketTypeRequests) {

        if(accountId <= 0) {throw new InvalidPurchaseException("Account id should start from 1");}

        var totalTicketCount= Arrays.stream(ticketTypeRequests)
                .mapToInt(TicketTypeRequest::noOfTickets)
                .sum();
        if(totalTicketCount > 20) {throw new InvalidPurchaseException("Only a maximum of 20 tickets that can be purchased at a time");}

        final Predicate<TicketTypeRequest> invalidTicketCountPredicate= p -> p.noOfTickets() <= 0;
        var invalidNoOfTicketsCount=  getCountByPredicate(invalidTicketCountPredicate,ticketTypeRequests);
        if(invalidNoOfTicketsCount >= 1) {throw new InvalidPurchaseException("Invalid no of tickets");}

        final Predicate<TicketTypeRequest> ticketTypeAdultPredicate = p -> p.type().equals(TicketTypeRequest.Type.ADULT);
        var adultTicketCount=getCountByPredicate(ticketTypeAdultPredicate,ticketTypeRequests);
        if(adultTicketCount == 0){throw new InvalidPurchaseException("Child and Infant tickets cannot be purchased without purchasing an Adult ticket");}
    }


    private static Integer lookUpTicketPrice(final TicketTypeRequest.Type ticketType)
    {return switch (ticketType){
        case INFANT -> 0;
        case CHILD -> 10;
        case ADULT -> 20;
      };
    }

    private Long getCountByPredicate(final Predicate<TicketTypeRequest>  predicate,final TicketTypeRequest... ticketTypeRequests){
        return Arrays.stream(ticketTypeRequests)
                .filter(predicate)
                .count();
    }


}

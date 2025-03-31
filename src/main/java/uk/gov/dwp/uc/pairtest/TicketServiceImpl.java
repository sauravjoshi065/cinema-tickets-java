package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;


/**
 * Handles ticket purchases while enforcing business rules:
 * - Validates requests before processing
 * - Calculates payments (adults £25, children £15, infants £0)
 * - Reserves seats (excluding infants)
 * - Integrates with external payment and seating services
 */

public class TicketServiceImpl implements TicketService {

    // Price constants
    private static final int MAX_TICKETS = 25;
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;
    private static final int INFANT_PRICE = 0;

    private final TicketPaymentService paymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService seatReservationService){
        this.paymentService = paymentService;
        this.seatReservationService =  seatReservationService;
    }


    /**
     * Processes a ticket purchase request after validation.
     * @throws InvalidPurchaseException if request violates business rules
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateAccountId(accountId);
        validateTicketRequests(ticketTypeRequests);
        
        int totalAmount =  calculateTotalAmount(ticketTypeRequests);
        int totalSeats =   calculateTotalSeats(ticketTypeRequests);

        paymentService.makePayment(accountId, totalAmount);
        seatReservationService.reserveSeat(accountId, totalSeats);

    }

    /**
     * Calculates seats needed (excludes infants as they sit on laps)
     */

    private int calculateTotalSeats(TicketTypeRequest[] ticketTypeRequests) {
        int seats = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request.getTicketType() != INFANT) {
                seats += request.getNoOfTickets();
            }
        }

        return seats;

    }

    private int calculateTotalAmount(TicketTypeRequest[] ticketTypeRequests) {
        int total = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT:
                    total += request.getNoOfTickets() * ADULT_PRICE;
                    break;
                case CHILD:
                    total += request.getNoOfTickets() * CHILD_PRICE;
                    break;
                case INFANT:
                    total += request.getNoOfTickets() * INFANT_PRICE;
                    break;
            }
        }

        return total;
    }

    /**
     * Validates:
     * - At least one adult when purchasing child/infant tickets
     * - Maximum 25 tickets
     * - No negative ticket counts
     * - Infants don't outnumber adults
     */

    private void validateTicketRequests(TicketTypeRequest[] ticketTypeRequests) throws InvalidPurchaseException {

        if(ticketTypeRequests == null || ticketTypeRequests.length == 0){
            throw new InvalidPurchaseException("No ticket requests provided");
        }

        int totalTickets = 0;
        boolean hasAdult = false;
        int infants = 0;
        int children = 0;
        int adults = 0;

        for(TicketTypeRequest request:ticketTypeRequests) {
            if(request == null){
                throw new InvalidPurchaseException("Null ticket request");
            }

            if (request.getNoOfTickets() <= 0){
                throw new InvalidPurchaseException("Ticket count must be positive");
            }

            totalTickets += request.getNoOfTickets();

            switch (request.getTicketType()){
                case ADULT :
                    adults += request.getNoOfTickets();
                    hasAdult = true;
                    break;
                case CHILD:
                    children += request.getNoOfTickets();
                    break;
                case INFANT:
                    infants += request.getNoOfTickets();
                    break;
            }
        }

        if(totalTickets > MAX_TICKETS){
            throw new InvalidPurchaseException("Maximum of " + MAX_TICKETS + " tickets exceeded");
        }

        if((infants > 0  || children > 0) && !hasAdult){
            throw new InvalidPurchaseException("Child and Infant tickets require at least one Adult ticket");
        }

        if(infants > adults){
            throw new InvalidPurchaseException("Not enough adults for infants");
        }
    }

    private static void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID");
        }
    }

}

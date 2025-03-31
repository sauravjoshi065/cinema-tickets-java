
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;


import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Tests {@link TicketServiceImpl} against all business rules and edge cases.
 * Uses mocking to isolate external services ({@link TicketPaymentService}, {@link SeatReservationService}).
 */
class TicketServiceImplTest {

    @Mock
    private TicketPaymentService paymentService;
    @Mock
    private SeatReservationService reservationService;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    // VALIDATION TESTS
    @Test
    void rejectInvalidAccountId() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(0L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1))
        );
    }

    @Test
    void rejectNullTicketRequests() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null)
        );
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void rejectZeroTickets() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L) // No tickets
        );
    }

    @Test
    void rejectMoreThanMaxTickets() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26))
        );
    }

    @Test
    void rejectChildTicketWithoutAdult() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1))
        );
    }

    @Test
    void rejectInfantTicketWithoutAdult() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1))
        );
    }

    @Test
    void rejectNegativeTicketCount() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1))
        );
    }

    @Test
    void rejectMoreInfantsThanAdults() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2) // 2 infants > 1 adult
                )
        );
    }

    //  PAYMENT CALCULATION TESTS
    @Test
    void calculateCorrectPaymentForAdultsOnly() throws InvalidPurchaseException {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3)
        );
        verify(paymentService).makePayment(1L, 3 * 25); // 3 Adults × £25
    }

    @Test
    void calculateCorrectPaymentForMixedTickets() throws InvalidPurchaseException {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)
        );
        verify(paymentService).makePayment(1L, (2 * 25) + (3 * 15)); // Adults + Children (Infants free)
    }

    // ===== SEAT RESERVATION TESTS =====
    @Test
    void reserveSeatsExcludingInfants() throws InvalidPurchaseException {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)
        );
        verify(reservationService).reserveSeat(1L, 2); // Only Adults get seats
    }

    @Test
    void reserveSeatsForAdultsAndChildren() throws InvalidPurchaseException {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2)
        );
        verify(reservationService).reserveSeat(1L, 3); // Adults + Children
    }

    //  EDGE CASES
    @Test
    void allowSingleAdultTicket() throws InvalidPurchaseException {
        assertDoesNotThrow(() ->
                ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1))
        );
    }

    @Test
    void allowMaxTickets() throws InvalidPurchaseException {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25)
        );
        verify(paymentService).makePayment(1L, 25 * 25);
    }
}
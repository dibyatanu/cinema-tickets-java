package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TicketServiceImplTest {
    @Mock
    private TicketPaymentService ticketPaymentService;
    @Mock
    private SeatReservationService seatReservationService;
    private TicketService ticketService;
    @BeforeEach
    public void setUp(){
        ticketService= new TicketServiceImpl(ticketPaymentService,seatReservationService);
    }

    @Nested
    class Success{
        @DisplayName("should successfully purchase 2 infant, 2 child and 2 adult tickets")
        @Test
        public void purchaseTickets_purchases_6_tickets(){
            var infantTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.CHILD);
            var adultTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.ADULT);
            ticketService.purchaseTickets(1L,infantTickets,childTickets,adultTickets);

            var accountIdCaptor= ArgumentCaptor.forClass(Long.class);
            var totalAmountToPayCaptor= ArgumentCaptor.forClass(Integer.class);

            verify(ticketPaymentService).makePayment(accountIdCaptor.capture(),totalAmountToPayCaptor.capture());
            assertThat(accountIdCaptor.getValue()).isEqualTo(1L);
            assertThat(totalAmountToPayCaptor.getValue()).isEqualTo(60);

            var totalSeatsToAllocateCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(seatReservationService).reserveSeat(accountIdCaptor.capture(),totalSeatsToAllocateCaptor.capture());
            assertThat(accountIdCaptor.getValue()).isEqualTo(1L);
            assertThat(totalSeatsToAllocateCaptor.getValue()).isEqualTo(4);
        }

        @DisplayName("should successfully purchase 2 infant, 8 child and 10 adult tickets")
        @Test
        public void purchaseTickets_purchases_20_tickets(){
            var infantTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(8, TicketTypeRequest.Type.CHILD);
            var adultTickets = new TicketTypeRequest(10, TicketTypeRequest.Type.ADULT);
            ticketService.purchaseTickets(1L,infantTickets,childTickets,adultTickets);

            var accountIdCaptor= ArgumentCaptor.forClass(Long.class);
            var totalAmountToPayCaptor= ArgumentCaptor.forClass(Integer.class);

            verify(ticketPaymentService).makePayment(accountIdCaptor.capture(),totalAmountToPayCaptor.capture());
            assertThat(accountIdCaptor.getValue()).isEqualTo(1L);
            assertThat(totalAmountToPayCaptor.getValue()).isEqualTo(280);

            var totalSeatsToAllocateCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(seatReservationService).reserveSeat(accountIdCaptor.capture(),totalSeatsToAllocateCaptor.capture());
            assertThat(accountIdCaptor.getValue()).isEqualTo(1L);
            assertThat(totalSeatsToAllocateCaptor.getValue()).isEqualTo(18);
        }
    }
    @Nested
    class ValidationFailure{
        @DisplayName("should throw exception for account id of 0")
        @Test
        public void purchaseTickets_account_id_zero(){
            var infantTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.CHILD);
            var adultTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.ADULT);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(0L,infantTickets,childTickets,adultTickets));
            assertThat(exception.getMessage()).isEqualTo("Account id should start from 1");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());

        }
        @DisplayName("should throw exception for account id of -1")
        @Test
        public void purchaseTickets_account_id_negative(){
            var infantTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.CHILD);
            var adultTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.ADULT);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(-1L,infantTickets,childTickets,adultTickets));
            assertThat(exception.getMessage()).isEqualTo("Account id should start from 1");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());

        }

        @DisplayName("should throw exception when total ticket count exceeds 20")
        @Test
        public void purchaseTickets_ticket_count_30(){
            var infantTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(8, TicketTypeRequest.Type.CHILD);
            var adultTickets = new TicketTypeRequest(20, TicketTypeRequest.Type.ADULT);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L,infantTickets,childTickets,adultTickets));
            assertThat(exception.getMessage()).isEqualTo("Only a maximum of 20 tickets that can be purchased at a time");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());
        }

        @DisplayName("should throw exception with tickets for infant ,child and no adults")
        @Test
        public void purchaseTickets_infant_child(){
            var infantTickets = new TicketTypeRequest(1, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(1, TicketTypeRequest.Type.CHILD);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L,infantTickets,childTickets));
            assertThat(exception.getMessage()).isEqualTo("Child and Infant tickets cannot be purchased without purchasing an Adult ticket");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());
        }

        @DisplayName("should throw exception with tickets for child only and no adults")
        @Test
        public void purchaseTickets_child(){
            var childTickets = new TicketTypeRequest(1, TicketTypeRequest.Type.CHILD);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L,childTickets));
            assertThat(exception.getMessage()).isEqualTo("Child and Infant tickets cannot be purchased without purchasing an Adult ticket");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());
        }

        @DisplayName("should throw exception with tickets for infant only and no adults")
        @Test
        public void purchaseTickets_infant(){
            var infantTickets = new TicketTypeRequest(1, TicketTypeRequest.Type.INFANT);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L,infantTickets));
            assertThat(exception.getMessage()).isEqualTo("Child and Infant tickets cannot be purchased without purchasing an Adult ticket");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());
        }

        @DisplayName("should throw exception when no of tickets is 0")
        @Test
        public void purchaseTickets_no_of_tickets_zero(){
            var infantTickets = new TicketTypeRequest(0, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(0, TicketTypeRequest.Type.CHILD);
            var adultTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.ADULT);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L,infantTickets,childTickets,adultTickets));
            assertThat(exception.getMessage()).isEqualTo("Invalid no of tickets");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());
        }

        @DisplayName("should throw exception when no of tickets is negative")
        @Test
        public void purchaseTickets_no_of_tickets_negative(){
            var infantTickets = new TicketTypeRequest(-1, TicketTypeRequest.Type.INFANT);
            var childTickets = new TicketTypeRequest(-5, TicketTypeRequest.Type.CHILD);
            var adultTickets = new TicketTypeRequest(2, TicketTypeRequest.Type.ADULT);
            var exception= Assertions.assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L,infantTickets,childTickets,adultTickets));
            assertThat(exception.getMessage()).isEqualTo("Invalid no of tickets");
            verify(ticketPaymentService,times(0)).makePayment(anyLong(),anyInt());
            verify(seatReservationService,times(0)).reserveSeat(anyLong(),anyInt());
        }
    }

}

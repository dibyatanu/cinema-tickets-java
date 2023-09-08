package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 */

public record TicketTypeRequest (
     Integer noOfTickets,
     Type type){
    public enum Type {
        ADULT, CHILD , INFANT
    }
}

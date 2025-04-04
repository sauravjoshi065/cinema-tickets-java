# Cinema Tickets Service

A Java implementation of a ticket purchasing system adhering to strict business rules and constraints.

## Features
- **Ticket Validation**: Enforces max 25 tickets, infant/adult ratios, and valid account IDs
- **Payment Calculation**: Correctly computes £25/adult, £15/child, £0/infant
- **Seat Reservation**: Automatically excludes infants from seat counts
- **Comprehensive Testing**: 100% business rule coverage with JUnit/Mockito

## Business Rules
| Rule | Implemented |
|------|------------|
| Max 25 tickets | ✔️ |
| Infant tickets require adults | ✔️ |
| Correct pricing (Adult £25, Child £15, Infant £0) | ✔️ |
| No seat allocation for infants | ✔️ |

## Constraints Met
-  No modification to `TicketService` interface
-  No changes to third-party services
-  Immutable `TicketTypeRequest` object

## How to Run
1. **Clone the repo**:
   ```bash
   git clone https://github.com/your-username/cinema-tickets-java.git
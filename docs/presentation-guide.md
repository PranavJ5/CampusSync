# CampusSync Phase 3 demonstration guide

This walkthrough demonstrates the intended end-to-end flow once the backend REST API, JWT security, and React/Vite frontend have been integrated. Use non-production demo accounts and sample data only.

## Before the demonstration

1. Start MySQL, the Spring Boot API, and the React/Vite frontend.
2. Confirm that an admin, a coordinator, and a student demo account exist.
3. Prepare at least one department and committee, plus a small poster image for the event.
4. Open the frontend at its Vite development URL and confirm the API is reachable.

## 1. Administrator: master data and health

1. Sign in with the administrator account.
2. Open the administrator dashboard.
3. Show aggregate cards for users, departments, committees, events, and registrations.
4. Navigate to departments and committees; verify the selected department and its committee details.
5. Open the system-health or API-status view, if it is enabled, and confirm the service reports healthy.

Expected outcome: the administrator can verify that the organisational data is available and the platform is ready for activity management.

## 2. Coordinator: draft, poster, and publication

1. Sign out and sign in as a coordinator who belongs to a committee.
2. Open the coordinator dashboard and choose **Create event**.
3. Enter the event title, description, category, venue, start/end time, registration deadline, and capacity.
4. Save the event with status **DRAFT**.
5. Upload the prepared poster and verify that its thumbnail appears in the draft.
6. Publish the event and confirm that the status becomes **PUBLISHED**.

Expected outcome: the event is visible in the catalogue with the correct hosting committee, poster, deadline, and seat capacity.

## 3. Student: discovery, registration, and notification

1. Sign out and sign in as a student.
2. On the student dashboard, point out the unread-notification count, registered-events summary, and committee memberships.
3. Open **Events**, select the published event, and show its poster, category, committee, registration deadline, and available seats.
4. Select **Register** and wait for the success message.
5. Return to the event detail page to confirm the remaining-seat count is updated.
6. Open **Notifications** and show the registration-confirmation notification. Mark it as read if required.

Expected outcome: the registration is created exactly once, the seat availability changes, and the student receives a confirmation.

## 4. Coordinator: participant list and attendance

1. Sign out and sign back in as the coordinator.
2. Open the event and choose the participant or registrations list.
3. Locate the student who just registered and show the updated registration total.
4. Mark the student as present after the event (or use the attendance control in the demo environment).
5. Confirm the attendance state is persisted after refreshing the list.

Expected outcome: coordinators can see current participants and record attendance against the event.

## Suggested questions and recovery points

- If an event is full, demonstrate the clear capacity-conflict message rather than retrying the request.
- If the registration deadline has passed, demonstrate the deadline-specific message.
- If a session expires, demonstrate the automatic return to the login screen and sign in again.
- If no records exist, use the corresponding empty-state message rather than a blank panel.

# NestEarth - Engineering Assumptions & Design Decisions

This document outlines the sensible technical and business assumptions made during the design and implementation of the NestEarth backend.

## 1. City & Localization Context
- **Default City**: Barwaha (Nimar region, Madhya Pradesh, India), with support for multi-city queries.
- **Currency**: Indian Rupees (₹ - INR). All rent and security deposit amounts are modeled as `BigDecimal` in whole or fractional rupees.
- **Phone Numbers**: 10-digit Indian mobile numbers (e.g., `9876543210` or `+919876543210`). Input normalization strips non-numeric characters and standardizes to 10 digits or E.164.

## 2. Authentication & Authorization
- **Role Selection**: At first login via OTP or Google OAuth, if the user does not have a designated role, `isNewUser: true` is returned and role is `null`. The client calls `PUT /api/v1/users/me/role` once to set either `TENANT` or `OWNER`. Once set, changing roles requires admin intervention or explicit profile settings.
- **OTP Security**:
  - Hashed using BCrypt/SHA-256 before storing in `otp_tokens`.
  - Expiry is strictly 5 minutes.
  - Maximum 5 failed attempts per OTP before invalidation.
  - Rate limited to maximum 3 OTP requests per 10 minutes per destination target (phone or email).
  - Dev profile uses fixed/deterministic OTP `123456` or logs generated OTP to standard output via `LoggingOtpSender`.
- **JWT Lifetimes**:
  - Access token: 15 minutes.
  - Refresh token: 30 days.
  - Refresh token rotation: Every use revokes the old refresh token and issues a new pair.

## 3. Privacy & Zero-Harassment Rules
- **Public Search**: Owner phone numbers are never returned in public listings (`/properties` or `/properties/{id}`). The owner card exposes only `id`, `name`, `avatarUrl`, and `ownerVerified` badge.
- **Enquiry Contact Reveal**: When an enquiry transitions to `ACCEPTED` status, the tenant can view the owner's phone number on `GET /enquiries/mine`.
- **Enquiry Rate Limiting**: A tenant cannot create more than one enquiry for the same property within a 24-hour rolling window.

## 4. Property Publishing & Verification
- **Validation on Publish**:
  - Minimum 3 valid uploaded images.
  - Rent > 0 and Deposit >= 0.
  - Valid configuration (`ONE_RK`, `ONE_BHK`, `TWO_BHK`, `THREE_BHK`, `FOUR_PLUS_BHK`).
  - Furnishing, Locality, and Address are mandatory.
- **Fast-Track Activation**:
  - If the owner has `owner_verified = true`, publishing sets property status directly to `ACTIVE`.
  - If the owner is not verified, status moves to `PENDING_REVIEW` for admin approval.

## 5. View Deduplication & Analytics
- View counts are recorded in `property_views`.
- To prevent spam and inflated numbers, views are deduplicated per viewer (authenticated user ID or client IP for anonymous users) per 24-hour calendar window.

## 6. Media Storage
- Presigned upload URLs are generated via `StorageService`.
- Allowed file types: `image/jpeg`, `image/png`, `image/webp`.
- Maximum file size: 5 MB (5,242,880 bytes).
- In `dev` profile, `LocalDevStorageService` creates direct file endpoints or mock presigned URLs, avoiding external AWS credentials during development and local testing.

## 7. Agreements & In-App Payments
- The application does not process payments inside the app (zero brokerage).
- Rental records and agreements record lease start dates, agreed rents, deposits, and platform fee percentages (default 0.0%) for audit and record-keeping purposes only.

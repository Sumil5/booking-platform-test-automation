package com.rbp.api.model;

import java.time.LocalDate;

public record BookingDates(LocalDate checkin, LocalDate checkout) {
}

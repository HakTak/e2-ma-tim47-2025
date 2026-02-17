package com.example.projekatmobilne.enums;

public enum TaskStatus {
    ACTIVE,    // Aktivan
    DONE,      // Urađen
    FAILED,    // Neurađen (sistemski/automatski)
    PAUSED,    // Pauziran (samo za ponavljajuće)
    CANCELLED,  // Otkazan
    UPCOMING  // Sto bi se reklo nadolazece obaveze
}
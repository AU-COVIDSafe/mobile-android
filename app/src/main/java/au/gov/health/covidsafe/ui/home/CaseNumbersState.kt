package au.gov.health.covidsafe.ui.home

enum class CaseNumbersState(state: Int) {
    LOADING(0),
    SUCCESS(1),
    ERROR_NO_NETWORK(2),
    ERROR_UNKNOWN(3),
}
package no.nav.aap.httpclient

class Header(val key: String, val value: String)
class FunctionalHeader(val key: String, val supplier: () -> String)

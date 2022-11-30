version = 1


cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "JioTV channels"
    authors = listOf("darkdemon")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Live",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=tv.googledrivelinks.com&sz=%size%"
}

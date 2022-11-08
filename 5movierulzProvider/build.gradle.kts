version = 1


cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "This website support English/Hindi/Bengali/Malayalam/Tamil/Telugu/Punjabi languages"
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
        "Movie",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=5movierulz.cm&sz=%size%"
}

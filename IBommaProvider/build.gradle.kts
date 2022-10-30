version = 2


cloudstream {
    language = "te"
    // All of these properties are optional, you can safely remove them

    // description = "Lorem Ipsum"
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
        "TvSeries",
        "Movie",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=ww1.ibomma.one/telugu-movies&sz=%size%"
}

package fr.banana_station.hubble_player;

class Song {

    private final long id;
    private final String title;
    private final String artist;

    Song(long id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    long getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    String getArtist() {
        return artist;
    }

}

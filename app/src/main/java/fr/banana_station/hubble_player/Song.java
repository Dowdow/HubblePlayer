package fr.banana_station.hubble_player;

class Song {

    private long id;
    private String title;
    private String artist;

    Song(long id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    public long getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    String getArtist() {
        return artist;
    }

}

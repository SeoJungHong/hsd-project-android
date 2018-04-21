package com.seojung.hsdproject;

/**
 * Created by SeoJung on 16. 6. 7..
 */
public class Song {

    private long id;
    private String title;
    private String artist;
    private boolean selected;

    public Song(long songID, String songTitle, String songArtist) {
        id = songID;
        title = songTitle;
        artist = songArtist;
        selected=false;
    }

    public long getID(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

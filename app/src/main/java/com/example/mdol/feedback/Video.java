package com.example.mdol.feedback;

public class Video
{
    String uriPath;
    String name;
    boolean isEnabled;
    Video(String uriPath,String name,boolean isEnabled)
    {
        this.uriPath = uriPath;
        this.name = name;
        this.isEnabled = isEnabled;
    }
}

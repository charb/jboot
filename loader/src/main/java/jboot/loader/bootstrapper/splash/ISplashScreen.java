package jboot.loader.bootstrapper.splash;

public interface ISplashScreen {

    void showSplashScreen();

    void removeSplashScreen();

    void setMaximumProgress(int maxValue);

    void incrementProgress(int progress);

    void setMessage(String message);
    
    void clearMessage();
    
}

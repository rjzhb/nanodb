package edu.caltech.nanodb.server;



public class ServerConfig {







/*
    public static void configStuff() {

    private void configureMaxCacheSize() {
        // Set the default up-front; it's just easier that way.
        maxCacheSize = DEFAULT_PAGECACHE_SIZE;

        String str = System.getProperty(PROP_PAGECACHE_SIZE);
        if (str != null) {
            str = str.trim().toLowerCase();

            long scale = 1;
            if (str.length() > 1) {
                char modifierChar = str.charAt(str.length() - 1);
                boolean removeModifier = true;
                if (modifierChar == 'k')
                    scale = 1024;
                else if (modifierChar == 'm')
                    scale = 1024 * 1024;
                else if (modifierChar == 'g')
                    scale = 1024 * 1024 * 1024;
                else
                    removeModifier = false;

                if (removeModifier)
                    str = str.substring(0, str.length() - 1);
            }



        try {
            maxCacheSize = Long.parseLong(str);
            maxCacheSize *= scale;
        }
        catch (NumberFormatException e) {
            logger.error(String.format(
                "Could not parse page-cache size value \"%s\"; " +
                    "using default value of %d bytes",
                System.getProperty(PROP_PAGECACHE_SIZE),
                DEFAULT_PAGECACHE_SIZE));

            maxCacheSize = DEFAULT_PAGECACHE_SIZE;
        }
    }
        */




}

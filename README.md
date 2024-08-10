<img src="https://github.com/user-attachments/assets/c21cba80-e742-4b52-9711-0dddbfefbc37" alt="spotnear-icon" width="100" height="100">

# SpotNear 🗺️🔍

SpotNear is an Android application that discovers interesting places near the user's location. It runs as a background service, periodically checking for points of interest (POIs) and notifying the user when it finds something interesting.

## Features 🌟

- 🏃‍♂️ Background service for continuous POI discovery
- 🗺️ Google Maps integration for visualizing discovered locations
- 🔔 Periodic notifications about interesting places nearby
- 🔍 On-demand POI search through persistent notification
- ⏱️ Customizable search intervals
- 💾 Persistence of discovered place details
- 🧪 Test mode for development and debugging
- 📏 User-defined search radius for POIs
- 🔄 Automatic display of the last discovered POI on app start

## Video Demonstrations 🎥

This video showcases the standard behavior and functionality of the SpotNear application:

1. App launch and permission granting
2. Starting the service
3. Receiving and interacting with POI notifications
4. Background operation and service persistence
5. Showing last save POI





https://github.com/user-attachments/assets/4812249c-6aac-4ade-853e-14854c83622f





This video demonstrates the functionality of the service when it automatically notifies about a nearby POI. It's shown in test mode to highlight the feature without long waiting times:

1. App launch and service start
2. Rapid POI discovery (every 5 seconds in test mode, 1 hour in non-test mode)
3. Notification interaction and map zooming
4. Continuous POI updates




https://github.com/user-attachments/assets/494f02c1-3f45-4d17-a263-eee55ad103f0






## Components 🧩

### MainActivity

The main entry point of the application. It handles:

- 🚀 Starting and stopping the SpotNear service
- 📍 Displaying current location and place details
- 🔐 Managing permissions
- 🗺️ Initializing the map fragment

### SpotNearService

A background service that:

- 📡 Periodically requests location updates
- 🔎 Searches for nearby POIs using the Overpass API
- 🔔 Sends notifications when interesting places are found
- 🔀 Supports test mode and non-test mode operations
- 📢 Provides a persistent notification for on-demand POI searches

### MapFragment

A fragment that displays a Google Map with:

- 🔍 The ability to zoom to specific coordinates
- 📌 Markers for discovered POIs

### PreferencesManager

A utility class for managing shared preferences, including:

- 💾 Saving and retrieving place details
- 🔄 Managing the service running state

### SpotNearServiceRestarter

A BroadcastReceiver that restarts the SpotNearService when the device reboots or the application is
updated.

## Dependencies 🛠️

- [LocationLibrary](https://github.com/bendayaniv/LocationLibrary) - A custom library for handling
  location-related functionalities
- Google Maps Android SDK
- OkHttp for network requests

## Setup 🚀

1. Clone the repository
2. Open the project in Android Studio
3. Add your Google Maps API key to the `AndroidManifest.xml` file
4. Build and run the application

## Usage 📱

1. Launch the app and grant necessary permissions
2. Enter the desired search radius for POIs in meters
3. Click the "Start Service" button to begin discovering nearby places
4. The app will notify you when it finds an interesting place
5. Click on the notification to view details about the discovered place
6. Use the "Stop Service" button to stop the background discovery process
7. At any time, click on the "SpotNear is running" notification to immediately search for new POIs without waiting for the next scheduled search
8. The last discovered POI will be automatically displayed when you reopen the app

## Customizable Search Radius 📏

SpotNear allows users to set their preferred search radius for discovering POIs:

- Users can input their desired search radius in meters before starting the service
- The search radius is persisted, so it's remembered across app restarts
- This feature allows users to customize the area in which they want to discover new places

## Persistent POI Display 🔄

The app now automatically displays the last discovered POI when reopened:

- The details of the last found interesting place are saved locally
- When the app is started, it checks for any previously discovered POI
- If a previous POI exists, its details are displayed and the map zooms to its location
- This feature ensures users don't lose track of interesting places they've discovered, even if they close the app

## Test Mode vs Non-Test Mode 🧪

SpotNear supports two operational modes:

### Test Mode

- Enabled by setting `TEST_MODE = true` in `SpotNearService`
- Uses a shorter interval (5 seconds) between POI searches
- Useful for development and debugging
- Allows for quicker testing of the POI discovery and notification system

### Non-Test Mode (Normal Operation)

- Enabled by setting `TEST_MODE = false` in `SpotNearService`
- Uses a longer interval (1 hour) between POI searches
- Designed for regular user experience
- Conserves battery and system resources

To switch between modes, modify the `TEST_MODE` constant in the `SpotNearService` class and rebuild
the application.

## On-Demand POI Search 🔍

While the service is running, a persistent notification with the title "SpotNear is running" is
displayed. This notification serves two purposes:

1. It informs the user that the SpotNear service is actively running in the background.
2. It provides a quick way to trigger an immediate POI search.

By clicking on this notification, the user can initiate an immediate search for new POIs without
waiting for the next scheduled search. This feature is particularly useful when the user changes
location and wants to discover new places right away.

## Acknowledgements 🙏

- OpenStreetMap and Overpass API for providing POI data
- Google for the Maps SDK


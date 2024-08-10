# SpotNear

SpotNear is an Android application that discovers interesting places near the user's location. It runs as a background service, periodically checking for points of interest (POIs) and notifying the user when it finds something interesting.

## Features

- Background service for continuous POI discovery
- Google Maps integration for visualizing discovered locations
- Periodic notifications about interesting places nearby
- On-demand POI search through persistent notification
- Customizable search intervals
- Persistence of discovered place details
- Test mode for development and debugging

## Components

### MainActivity

The main entry point of the application. It handles:
- Starting and stopping the SpotNear service
- Displaying current location and place details
- Managing permissions
- Initializing the map fragment

### SpotNearService

A background service that:
- Periodically requests location updates
- Searches for nearby POIs using the Overpass API
- Sends notifications when interesting places are found
- Supports test mode and non-test mode operations
- Provides a persistent notification for on-demand POI searches

### MapFragment

A fragment that displays a Google Map with:
- The ability to zoom to specific coordinates
- Markers for discovered POIs

### PreferencesManager

A utility class for managing shared preferences, including:
- Saving and retrieving place details
- Managing the service running state

### SpotNearServiceRestarter

A BroadcastReceiver that restarts the SpotNearService when the device reboots or the application is updated.

## Dependencies

- [LocationLibrary](https://github.com/bendayaniv/LocationLibrary) - A custom library for handling location-related functionalities
- Google Maps Android SDK
- OkHttp for network requests

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Add your Google Maps API key to the `AndroidManifest.xml` file
4. Build and run the application

## Usage

1. Launch the app and grant necessary permissions
2. Click the "Start Service" button to begin discovering nearby places
3. The app will notify you when it finds an interesting place
4. Click on the notification to view details about the discovered place
5. Use the "Stop Service" button to stop the background discovery process
6. At any time, click on the "SpotNear is running" notification to immediately search for new POIs without waiting for the next scheduled search

## Test Mode vs Non-Test Mode

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

To switch between modes, modify the `TEST_MODE` constant in the `SpotNearService` class and rebuild the application.

## On-Demand POI Search

While the service is running, a persistent notification with the title "SpotNear is running" is displayed. This notification serves two purposes:

1. It informs the user that the SpotNear service is actively running in the background.
2. It provides a quick way to trigger an immediate POI search.

By clicking on this notification, the user can initiate an immediate search for new POIs without waiting for the next scheduled search. This feature is particularly useful when the user changes location and wants to discover new places right away.

## Acknowledgements

- OpenStreetMap and Overpass API for providing POI data
- Google for the Maps SDK
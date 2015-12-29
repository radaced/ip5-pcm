package ch.fhnw.ip5.powerconsumptionmanager.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ch.fhnw.ip5.powerconsumptionmanager.R;
import ch.fhnw.ip5.powerconsumptionmanager.model.PlanEntryModel;
import ch.fhnw.ip5.powerconsumptionmanager.model.RouteInformationModel;
import ch.fhnw.ip5.powerconsumptionmanager.network.DataLoader;
import ch.fhnw.ip5.powerconsumptionmanager.network.DataLoaderCallback;
import ch.fhnw.ip5.powerconsumptionmanager.view.PlanFragment;

/**
 * Helper class to handle and modify caldroid
 */
public class PlanHelper implements DataLoaderCallback {
    // Projection array holding values to read from the instances table
    private static final String[] INSTANCE_FIELDS = new String[] {
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
    };

    // Projection array indices
    private static final int INSTANCE_TITLE = 0;
    private static final int INSTANCE_EVENT_LOCATION = 1;
    private static final int INSTANCE_DESCRIPTION = 2;
    private static final int INSTANCE_BEGIN = 3;
    private static final int INSTANCE_END = 4;

    // The caldroid fragment itself
    private CaldroidFragment mCaldroid;
    // Contexts
    private PlanFragment mContext;
    private PowerConsumptionManagerAppContext mAppContext;
    // Calendar instance to make date operations
    private Calendar mCalendar;
    // Holds the read instances from the calendar.instances table of one month
    private HashMap<Integer, PlanEntryModel> mInstances;
    // The actual selected date in the caldroid fragment
    private Date mSelectedDate = new Date();


    public PlanHelper(CaldroidFragment caldroid, PlanFragment context) {
        mCaldroid = caldroid;
        mContext = context;
        mAppContext = (PowerConsumptionManagerAppContext) mContext.getActivity().getApplicationContext();
        mInstances = new HashMap<Integer, PlanEntryModel>();
    }

    // Initial settings for the caldroid fragment
    public void setup() {
        mCalendar = Calendar.getInstance();
        Bundle args = new Bundle();
        args.putInt(CaldroidFragment.MONTH, mCalendar.get(Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, mCalendar.get(Calendar.YEAR));
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        args.putInt(CaldroidFragment.THEME_RESOURCE, R.style.CustomCaldroidTheme);
        args.putBoolean(CaldroidFragment.SHOW_NAVIGATION_ARROWS, false);
        args.putBoolean(CaldroidFragment.ENABLE_CLICK_ON_DISABLED_DATES, false);
        args.putBoolean(CaldroidFragment.SQUARE_TEXT_VIEW_CELL, false);
        mCaldroid.setArguments(args);
    }

    // Generates the lower range from the range the calendar.instances table should be read (start of month)
    public long generateLowerMonthRangeEnd(int year, int month) {
        mCalendar = Calendar.getInstance();
        mCalendar.set(year, month, 1, 0, 0, 0);
        return mCalendar.getTimeInMillis();
    }

    // Generates the upper range from the range the calendar.instances table should be read (end of month)
    public long generateUpperMonthRangeEnd(int year, int month) {
        mCalendar = Calendar.getInstance();
        mCalendar.set(year, month, 1, 23, 59, 59);
        mCalendar.set(year, month, mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return mCalendar.getTimeInMillis();
    }

    // Reads all planned tesla trips from the calendar.instances table
    public void readPlannedTrips(long lowerRangeEnd, long upperRangeEnd) {
        ContentResolver cr = mContext.getActivity().getContentResolver();
        // Condition what entries in the instance table to read
        String selection = "((" + CalendarContract.Instances.BEGIN + " >= ?) AND (" + CalendarContract.Instances.END + " <= ?))";
        // Arguments for the condition (replacing ?)
        String[] selectionArgs = new String[]{String.valueOf(lowerRangeEnd), String.valueOf(upperRangeEnd)};

        // Build the uri
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, lowerRangeEnd);
        ContentUris.appendId(builder, upperRangeEnd);

        // Submit query
        Cursor cursor = cr.query(builder.build(), INSTANCE_FIELDS, selection, selectionArgs, null);

        // Iterate through results
        while (cursor.moveToNext()) {
            String title = cursor.getString(INSTANCE_TITLE);

            // Check if tesla trip or not
            if(!title.equals(mContext.getString(R.string.instance_title))) {
                continue;
            }

            String eventLocation = cursor.getString(INSTANCE_EVENT_LOCATION);
            String description = cursor.getString(INSTANCE_DESCRIPTION);
            long begin = cursor.getLong(INSTANCE_BEGIN);
            mCalendar.setTimeInMillis(begin);
            int startDay = mCalendar.get(Calendar.DAY_OF_MONTH);
            long end = cursor.getLong(INSTANCE_END);

            // Store read data into hash map
            if(!mInstances.containsKey(startDay)) {
                mInstances.put(startDay, new PlanEntryModel(title, eventLocation, description, new Date(begin), new Date(end)));
            }
        }
        cursor.close();
    }

    // Mark all dates in the caldroid fragment that have a tesla trip instance
    public void markDays() {
        Iterator iterator = mInstances.entrySet().iterator();

        /* TODO foreach */
        // Iterate through all read calendar instances
        while(iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            PlanEntryModel pem = (PlanEntryModel) pair.getValue();
            mCalendar.setTime(pem.getBegin());
            mCaldroid.setSelectedDate(mCalendar.getTime());
        }

        // Update caldroid fragment
        mCaldroid.refreshView();
    }

    // Define the listener for the caldroid fragment
    public void generateListener() {
        // Date format masks to display dates
        final SimpleDateFormat titleFormat = new SimpleDateFormat(mContext.getString(R.string.format_caldroid_info_title));
        final SimpleDateFormat timeRangeFormat = new SimpleDateFormat(mContext.getString(R.string.format_caldroid_info_timerange));

        CaldroidListener listener = new CaldroidListener() {
            @Override
            public void onSelectDate(Date date, View view) {
                mCalendar.setTime(date);
                int pressedDay = mCalendar.get(Calendar.DAY_OF_MONTH);

                // Mark selected day with accent color and load data to the instance on that day
                if(mInstances.containsKey(pressedDay) && !mSelectedDate.equals(date)) {
                    // Modify look and feel
                    mCaldroid.setBackgroundResourceForDate(R.drawable.caldroid_selected_day, date);
                    mCaldroid.clearBackgroundResourceForDate(mSelectedDate);
                    mSelectedDate = date;
                    mCaldroid.refreshView();

                    // Display instance data
                    PlanEntryModel pem = mInstances.get(pressedDay);
                    View v = mContext.getView();
                    // Title
                    TextView title = (TextView) v.findViewById(R.id.caldroid_info_title);
                    title.setText(titleFormat.format(pem.getBegin()));
                    // Description
                    TextView description = (TextView) v.findViewById(R.id.caldroid_info_description);
                    if(!pem.getDescription().equals("")) {
                        description.setText(pem.getDescription());
                    } else {
                        description.setText(mContext.getString(R.string.text_information_no_description));
                    }
                    description.setBackgroundResource(R.color.colorTextViewBackground);
                    description.setMovementMethod(ScrollingMovementMethod.getInstance());
                    // Time range of instance
                    TextView timeRange = (TextView) v.findViewById(R.id.caldroid_info_timerange);
                    timeRange.setText(timeRangeFormat.format(pem.getBegin()) + " - " + timeRangeFormat.format(pem.getEnd()));

                    // Load distance and duration to reach destination between two given locations from the instance
                    String[] locations = pem.getEventLocation().split("/");
                    if(locations != null && !"".equals(locations[0]) && !"".equals(locations[1])) {
                        calculateDistance(locations[0].trim(), locations[1].trim());
                    } else {
                        displayRouteInformation(v, mContext.getString(R.string.text_route_information_no_data), "", false);
                    }
                }
            }

            @Override
            public void onChangeMonth(int month, int year) {
                /*
                 * Read calendar.instances table with new range and mark days that contain an instance
                 * -1 because caldroid calculates months from 1-12 and Calendar.class does it with 0-11
                 */
                long startMonth = generateLowerMonthRangeEnd(year, month-1);
                long endMonth = generateUpperMonthRangeEnd(year, month-1);
                mInstances.clear();
                readPlannedTrips(startMonth, endMonth);
                markDays();
            }
        };

        mCaldroid.setCaldroidListener(listener);
    }

    public CaldroidFragment getCaldroid() {
        return mCaldroid;
    }

    @Override
    public void DataLoaderDidFinish() {
        // Update the text view field for the route information with the loaded data on the UI thread
        mContext.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = mContext.getView();
                RouteInformationModel rim = mAppContext.getRouteInformation();

                // Check if a route existed
                if(rim.getDistanceText().equals("")) {
                    displayRouteInformation(view, mContext.getString(R.string.text_route_information_no_route), "", false);
                } else {
                    displayRouteInformation(
                        view,
                        mContext.getString(R.string.text_route_information_distance) + " " + rim.getDistanceText(),
                        mContext.getString(R.string.text_route_information_duration) + " " + rim.getDurationText(),
                        true
                    );
                }
            }
        });
    }

    @Override
    public void DataLoaderDidFail() {
        // Update the text view field for the route information with the error message on the UI thread
        mContext.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = mContext.getView();
                displayRouteInformation(view, mContext.getString(R.string.text_route_information_error), "", false);
            }
        });
    }

    /*
     * Call google.maps API with the origin and destination location to find out distance and duration
     * of the planned trip
     */
    private void calculateDistance(String origin, String destination) {
        DataLoader loader = new DataLoader(mAppContext, this);
        loader.loadRouteInformation(
            mContext.getString(R.string.googleMaps_Api1) +
            "origin=" + origin +
            "&destination=" + destination +
            mContext.getString(R.string.googleMaps_Api2)
        );
    }

    /* Displays the route information (error, no route available or loaded information). On errors
     * one of the fields width is minimized so a longer error-message can be shown.
     */
    private void displayRouteInformation(View v, String distance, String duration, boolean valid) {
        TextView routeDistance = (TextView) v.findViewById(R.id.text_route_information_distance);
        TextView routeDuration = (TextView) v.findViewById(R.id.text_route_information_duration);

        if(valid) {
            routeDistance.setText(distance);
            routeDuration.setText(duration);
            routeDuration.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                1f
            ));
        } else {
            routeDistance.setText(distance);
            routeDuration.setText("");
            routeDuration.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                0f
            ));
        }
    }
}

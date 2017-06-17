package com.example.android.comp7081_cameraapp;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SelectionActivity extends ListActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra(MainActivity.ACTIVITY_TITLE_EXTRA);
            String[] selections = intent.getStringArrayExtra(MainActivity.SELECTIONS_EXTRA);
            String strCityFilter = MainActivity.strCityFilter;
            String strTagFilter = MainActivity.strTAGFilter;
            String strDateFilter = MainActivity.strDateFilter;
            String strFromDateFilter = MainActivity.strFromDateFilter;
            String strToDateFilter = MainActivity.strToDateFilter;

            boolean hasCityFilter = (strCityFilter!=null && !strCityFilter.isEmpty()) ? true : false;
            boolean hasTagFilter = (strTagFilter!=null && !strTagFilter.isEmpty()) ? true : false;
            boolean hasDateFilter = (strDateFilter!=null && !strDateFilter.isEmpty()) ? true : false;
            boolean hasDateRangeFilter = ( (strFromDateFilter!=null && !strFromDateFilter.isEmpty())
                    && (strToDateFilter!=null && !strToDateFilter.isEmpty()) ) ? true : false;

            // Add extension to Date filter if it is not empty / null
            if (hasDateFilter)
            {
                strDateFilter = strDateFilter + ".jpg";;
            }

            if (title != null)
                setTitle(title);

            if(title.equalsIgnoreCase("Select picture for thumbnail"))
            {
                // For Filtering
                List<String> listForFiltering =  new ArrayList<String>();
                // Iterate through all files to find a match.
                for (String s: selections)
                {

                    // Date range filter takes precendence over all filters
                    if ( hasDateRangeFilter )
                    {

                        DateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
                        try {

                            Date fromDate = df.parse(strFromDateFilter);
                            Date toDate = df.parse(strToDateFilter);

                            Calendar start = Calendar.getInstance();
                            start.setTime(fromDate);
                            Calendar end = Calendar.getInstance();
                            end.setTime(toDate);
                            end.add(Calendar.DATE, 1);


                            for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {

                                String strCurrentDateForFilter = df.format(date)+ ".jpg";
                                if ( s.endsWith(strCurrentDateForFilter) )
                                {
                                    listForFiltering.add(s);
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    // City filter, no date filter, no TAG filter
                    else if ( (hasCityFilter)  && (!hasDateFilter) && (!hasTagFilter))
                    {
                        if (s.startsWith (strCityFilter))
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // Date filter, no city filter, no TAG filter
                    else if ( (!hasCityFilter)  && (hasDateFilter) && (!hasTagFilter))
                    {
                        if (s.endsWith (strDateFilter))
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // Date filter & city filter, no TAG filter
                    else if ( (hasCityFilter)  && (hasDateFilter) && (!hasTagFilter) )
                    {
                        if ( (s.startsWith(strCityFilter)) && (s.endsWith(strDateFilter)) )
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // Date filter, city filter, & TAG filter
                    else if ( (hasCityFilter)  && (hasDateFilter) && (hasTagFilter) )
                    {
                        if ( (s.startsWith(strCityFilter)) && (s.endsWith(strDateFilter) && (s.contains(strTagFilter)) ) )
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // Date filter, city filter, & TAG filter
                    else if ( (hasCityFilter)  && (hasDateFilter) && (hasTagFilter) )
                    {
                        if ( (s.startsWith(strCityFilter)) && (s.endsWith(strDateFilter) && (s.contains(strTagFilter)) ) )
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // Date & TAG  filter, no city filter
                    else if ( (!hasCityFilter)  && (hasDateFilter) && (hasTagFilter) )
                    {
                        if ( (s.endsWith(strDateFilter) && (s.contains(strTagFilter)) ) )
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // TAG  filter, no city or date filter
                    else if ( (!hasCityFilter)  && (!hasDateFilter) && (hasTagFilter) )
                    {
                        if ( (s.contains(strTagFilter)) )
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // City and TAG filter, no date filter
                    else if ( (hasCityFilter)  && (!hasDateFilter) && (hasTagFilter) )
                    {
                        if ( (s.startsWith(strCityFilter)) && (s.contains(strTagFilter)) )
                        {
                            listForFiltering.add(s);
                        }
                    }
                    // No filters
                    else
                    {
                        listForFiltering.add(s);
                    }

                }

                String[] selectionsFiltered = new String[listForFiltering.size()];
                listForFiltering.toArray(selectionsFiltered);

                if(selectionsFiltered != null) {
                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<String>(this, R.layout.simple_list_item_1, selectionsFiltered);
                    setListAdapter(adapter);
                }

                // Reset filters
                MainActivity.strCityFilter = null;
                MainActivity.strTAGFilter = null;
                MainActivity.strDateFilter = null;
                MainActivity.strFromDateFilter = null;
                MainActivity.strToDateFilter = null;
            }
            else
            {
                if(selections != null) {
                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<String>(this, R.layout.simple_list_item_1, selections);
                    setListAdapter(adapter);
                }
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(MainActivity.SELECTED_INDEX_EXTRA, position);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
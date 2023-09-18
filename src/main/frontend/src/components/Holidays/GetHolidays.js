import React, {useEffect} from 'react';
import {useConfig} from '../Context/Config.js';
import useSWR from "swr";

/**
 * GetHolidays fetcher by year hook
 * @param year
 * @returns {{isLoading: boolean, holidayEventsData: any, getHolidayEventsError: any}}
 */
const useFetchByYear = (year) => {
    const {apiUrl} = useConfig();
    const eventFetcher = (url) => fetch(url).then((res) => res.json());
    const {data, error, isLoading} = useSWR(`${apiUrl}/${year}`, eventFetcher);

    return {
        holidayEventsData: data,
        getHolidayEventsError: error,
        isLoading
    };
};

const fetchItems = async (url) => {
    // fetch holiday items
    const response = await fetch(url);
    return await response.json();
};


/**
 * GetHolidays fetcher component to get events by year
 */
function GetHolidays({yearCalendarState, setYearCalendarState}) {

    // [!] Solution with SWR
    // There is a defect: initially we have to select another year and then select the current one
    // then, the fetch will be triggered
    // So, needs to be fixed
    // const {holidayEventsData, getHolidayEventsError, isLoading} = useFetchByYear(yearCalendarState.selectedYear);
    //
    // useEffect(() => {
    //     console.log(holidayEventsData, getHolidayEventsError)
    //     if (getHolidayEventsError) {
    //         console.error(getHolidayEventsError);
    //     } else {
    //         setYearCalendarState({...yearCalendarState, holidayEvents: holidayEventsData});
    //     }
    // }, [yearCalendarState.selectedYear]);


    // [!] Solution with pure fetch()
    const {apiUrl} = useConfig();

    useEffect(() => {
        fetchItems(`${apiUrl}/${yearCalendarState.selectedYear}`)
            .then((data) => {
                if (data.error) {
                    console.error(data.error);
                }
                else {
                    setYearCalendarState({...yearCalendarState, holidayEvents: data});
                }
            });
    }, [yearCalendarState.selectedYear]);

}

export default GetHolidays;
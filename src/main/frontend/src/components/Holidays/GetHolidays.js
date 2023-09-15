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
 * GetHolidays fetcher component
 */
function GetHolidays({yearCalendarState, setYearCalendarState}) {
    // todo: how to use SWR inside useEffect
    // const {holidayEventsData, getHolidayEventsError, isLoading} = useFetchByYear(yearCalendarState.selectedYear);

    const {apiUrl} = useConfig();

    // get events by year
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
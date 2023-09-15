import {useEffect} from "react";
import useSWR, {mutate} from "swr";
import {useConfig} from "../Context/Config";


const POST_VERB = "POST";
const PUT_VERB = "PUT";
const DEL_VERB = "DELETE";

const handleAddUpdateDeleteItem = async (url, holiday, verb) => {
    // create a new item
    const response = await fetch(url, {
        method: verb,
        body: JSON.stringify(holiday),
        headers: {
            'Content-Type': 'application/json'
        },
    });

    switch (verb) {
        case POST_VERB:
        case PUT_VERB:
            return await response.json();
        case DEL_VERB:
            return response;
    }
};

const processOutput = (data, callback) => {
    if (data.error || (data.holidays && data.holidays.length === 0)) {
        console.error(data.error, data.message);
        console.log("Error: " + data.message, "Status: " + data.status);
    } else {
        console.info(data.message);
        callback(data);
    }
};

/**
 * Add, Update, Delete holiday on back-end
 */
function ModifyHolidays({holidayDTO, callbacks}) {

    const {apiUrl} = useConfig();

    useEffect(() => {

        if (holidayDTO.add) {
            // insert Holiday
            handleAddUpdateDeleteItem(`${apiUrl}/holiday`, holidayDTO.holiday, POST_VERB)
                .then((data) =>
                    processOutput(data, callbacks.onCreate)
                );
        } else if (holidayDTO.update) {
            // update Holiday
            handleAddUpdateDeleteItem(`${apiUrl}/holiday`, holidayDTO.holiday, PUT_VERB)
                .then((data) =>
                    processOutput(data, callbacks.onUpdate)
                );
        } else if (holidayDTO.remove) {
            // delete Holiday
            handleAddUpdateDeleteItem(`${apiUrl}/holiday`, holidayDTO.holiday, DEL_VERB)
                .then((data) =>
                    processOutput(data, callbacks.onDelete)
                );
        }

        callbacks.onFinish();

    }, [holidayDTO]);
}

export default ModifyHolidays;
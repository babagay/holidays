import React, {useState, useEffect, useRef} from 'react';
import './Dialog.css'
import classNames from "classnames";

function Dialog({isOpen, isSaveBtn, isDeleteBtn, onClose, children, handleSaveButton, handleDeleteButton}) {
    const [isModalOpen, setModalOpen] = useState(isOpen);
    const [isSaveBtnDisplay, setIsSaveBtnDisplay] = useState(isSaveBtn);
    const [isDeleteBtnDisplay, setIsDeleteBtnDisplay] = useState(isDeleteBtn);
    const modalRef = useRef(null);

    const saveBtnClass =
        classNames('Dialog_Button', {
            'Save_Button': true,
            'Hidden': !isSaveBtnDisplay
        });

    const deleteBtnClass = classNames('Dialog_Button', {
        'Delete_Button': true,
        'Hidden': !isDeleteBtnDisplay
    });

    const handleCloseModal = () => {
        if (onClose) {
            onClose();
        }
        setModalOpen(false);
    };

    const handleSaveBtn = (event) => {
        if (isSaveBtnDisplay && handleSaveButton) {
            handleSaveButton(event);
        }
    };
    const handleDeleteBtn = (event) => {
        if (handleDeleteButton) {
            handleDeleteButton(event);
        }
    };

    useEffect(() => {
        setIsSaveBtnDisplay(isSaveBtnDisplay);
    }, [isSaveBtnDisplay]);

    useEffect(() => {
        setIsDeleteBtnDisplay(isDeleteBtn);
    }, [isDeleteBtn]);

    useEffect(() => {
        setModalOpen(isOpen);
    }, [isOpen]);

    useEffect(() => {
        const modalElement = modalRef.current;

        if (modalElement) {
            if (isModalOpen) {
                modalElement.showModal();
            } else {
                modalElement.close();
            }
        }
    }, [isModalOpen]);

    return (
        <dialog ref={modalRef} className="Modal">
            <div className="Dialog_Content">
                {children}
            </div>
            <button className="Close_Button" onClick={handleCloseModal}>
                Close
            </button>
            <button className={saveBtnClass} onClick={handleSaveBtn}>Save</button>
            <button className={deleteBtnClass}
                    onClick={handleDeleteBtn}>Delete
            </button>
        </dialog>
    );

}

export default Dialog;
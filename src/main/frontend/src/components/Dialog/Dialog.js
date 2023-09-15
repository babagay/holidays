import React, {useState, useEffect, useRef} from 'react';
import './Dialog.css'

function Dialog({isOpen, isSaveBtn, isDeleteBtn, onClose, children, handleSaveButton, handleDeleteButton}) {
    const [isModalOpen, setModalOpen] = useState(isOpen);
    const [isSaveBtnDisplay, setIsSaveBtnDisplay] = useState(isSaveBtn);
    const [isDeleteBtnDisplay, setIsDeleteBtnDisplay] = useState(isDeleteBtn);
    const modalRef = useRef(null);

    const saveBtnClass = `dialog-button save-button ${isSaveBtnDisplay ? '' : 'hidden'}`;
    const deleteBtnClass = `dialog-button delete-button ${isDeleteBtnDisplay ? '' : 'hidden'}`;

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
        <dialog ref={modalRef} className="modal">
            <div className="dialog-content">
                {children}
            </div>
            <button className="close-button" onClick={handleCloseModal}>
                Close
            </button>
            <button className={saveBtnClass} onClick={handleSaveBtn}>Save</button>
            <button className={deleteBtnClass} onClick={handleDeleteBtn}>Delete</button>
        </dialog>
    );

}

export default Dialog;
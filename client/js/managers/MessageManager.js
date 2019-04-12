export default class MessageManager {

    constructor(axiosApi) {
        this.messages = [];
        this.message = {};
        this.axios = axiosApi;
    }

    fetchAllMessages(url, code) {
        return this.axios.get(url + "/allMessages?code=" + code)
            .then((response) => {
                this.messages = response.data;
            });
    }
    fetchMessages(url, code, label) {
        return this.axios.get(url + "/messages?code=" + code + "&label=" + label)
            .then((response) => {
                this.messages = response.data;
            });
    }

    fetchMessage(url, code, id) {
        return this.axios.get(url + "/message?code=" + code + "&id=" + id)
            .then((response) => {
                this.message = response.data;
            });
    }
    sendMessage(url, code, to, subject, bodyText) {
        return this.axios.post(url + "/send?code=" + code, {
            to: to,
            subject: subject,
            bodyText: bodyText
        }).then((response) => {
            this.message = response.data;
        });
    }

    getMessages() {
        return this.messages;
    }

    getMessage() {
        return this.message;
    }
}
export default class MessageManager {

    constructor(axiosApi) {
        this.messages = [];
        this.message = {};
        this.axios = axiosApi;
    }

    fetchMessages(url, code) {
        return this.axios.get(url + "/messages?code=" + code)
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

    getMessages() {
        return this.messages;
    }

    getMessage() {
        return this.message;
    }
}
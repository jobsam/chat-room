(ns chat-room.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [org.httpkit.server :as http-kit]
            [taoensso.timbre :as timbre]))

;; Initialize Sente
(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom

;; Event handler
(defmulti event-msg-handler :id) ; Dispatch on event-id

;; Default/fallback case (no other matching handler)
(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (timbre/debugf "Unhandled event: %s" event)
  (when ?reply-fn
    (?reply-fn {:umatched-event-as-echoed-from-server event})))

;; Handle :chat/message events
(defmethod event-msg-handler :chat/message
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [{:keys [message]} ?data]
    (timbre/debugf "Received message: %s" message)
    ;; Broadcast the message to all connected clients
    (doseq [uid (:any @connected-uids)]
      (chsk-send! uid [:chat/message {:message message}]))))

;; Sente event router
(defonce router_ (atom nil))

(defn stop-router! [] 
  (when-let [stop-fn @router_] (stop-fn)))

(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk event-msg-handler)))

;; Routes
(defroutes app-routes
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

;; Server
(defonce server (atom nil))

(defn stop-server! []
  (when-let [stop-fn @server]
    (stop-fn :timeout 100)
    (reset! server nil)))

(defn start-server! [port]
  (stop-server!)
  (timbre/info "Starting server on port" port)
  (reset! server (http-kit/run-server #'app {:port port}))
  (start-router!))

(defn -main [& args]
  (start-server! 3000))

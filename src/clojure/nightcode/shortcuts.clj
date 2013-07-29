(ns nightcode.shortcuts
  (:require [nightcode.utils :as utils]
            [seesaw.core :as s]
            [seesaw.keymap :as keymap])
  (:import [java.awt Color KeyboardFocusManager KeyEventDispatcher]
           [java.awt.event ActionEvent KeyEvent]
           [net.java.balloontip BalloonTip]
           [net.java.balloontip.positioners CenteredPositioner]
           [net.java.balloontip.styles ToolTipBalloonStyle]))

(def ^:const mappings
  {:new-project-button "P"
   :new-file-button "N"
   :rename-file-button "M"
   :import-button "O"
   :remove-button "I"
   :run-button "R"
   :run-repl-button "E"
   :build-button "B"
   :test-button "T"
   :clean-button "L"
   :stop-button "U"
   :save-button "S"
   :undo-button "Z"
   :redo-button "Y"
   :repl-console "G"
   :find-field "F"
   :close-button "W"
   :project-tree "↑ ↓ ↵"
   :toggle-logcat-button "S"})

(defn create-mappings
  [panel pairs]
  (doseq [[id func] pairs]
    (when-let [mapping (get mappings id)]
      (keymap/map-key panel
                      (str "control " mapping)
                      (fn [e]
                        ; only run the function if the button is enabled
                        (when (-> panel
                                  (s/select [(keyword (str "#" (name id)))])
                                  (s/config :enabled?))
                          (func e)))
                      :scope :global))))

(defn is-visible?
  [widget]
  (if widget
    (and (.isVisible widget)
         (is-visible? (.getParent widget)))
    true))

(defn toggle-hints
  [target show?]
  (doseq [hint (s/select target [:BalloonTip])]
    (if (and show? (is-visible? (.getAttachedComponent hint)))
      (s/show! hint)
      (s/hide! hint))))

(defn create-hint
  [btn text]
  (when text
    (let [style (ToolTipBalloonStyle. Color/DARK_GRAY Color/DARK_GRAY)
          positioner (CenteredPositioner. 0)]
      (.enableFixedAttachLocation positioner true)
      (.setAttachLocation positioner 0.5 0.5)
      (doto (BalloonTip. btn text style false)
        (.setPositioner positioner)
        (.setVisible false)))))

(defn create-hints
  [target]
  ; create hints and initially hide them
  (doseq [[id mapping] mappings]
    (when-let [btn (s/select target [(keyword (str "#" (name id)))])]
      (create-hint btn mapping)))
  ; set custom key events
  (.addKeyEventDispatcher
    (KeyboardFocusManager/getCurrentKeyboardFocusManager)
    (proxy [KeyEventDispatcher] []
      (dispatchKeyEvent [e]
        (toggle-hints target (.isControlDown e))
        (if (and (.isControlDown e) (= (.getID e) KeyEvent/KEY_PRESSED))
          (case (.getKeyCode e)
            ; enter
            10 (utils/toggle-project-tree-selection)
            ; up
            38 (utils/move-project-tree-selection -1)
            ; down
            40 (utils/move-project-tree-selection 1)
            ; Q
            81 (utils/shut-down)
            false)
          false))))
  ; return target
  target)

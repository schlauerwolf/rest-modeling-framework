<?php
/**
 * This file has been auto generated
 * Do not change it
 */

namespace Test\Types;

use Test\Base\JsonObjectModel;

class ContainerModel extends JsonObjectModel implements Container {
    private $patternData = [];
    /**
     * @param string $key
     * @return mixed
     */
    public function get($key)
    {
        if(!isset($this->patternData[$key])) {
            switch (true) {
                case preg_match('//', $key):
                    $value = $this->raw($key);
                    break;
                default:
                    throw new \InvalidArgumentException();
            }
            $this->patternData[$key] = $value;
        }
        return $this->patternData[$key];
    }

    /**
     * @param string $key
     * @param mixed $value
     * @return $this
     */
    public function set($key, $value)
    {
        if (!$this->validKey($key)) {
            throw new \InvalidArgumentException();
        }
        $this->patternData[$key] = $value;
        return $this;
    }


    private function validKey($key)
    {
        switch (true) {
            case preg_match('//', $key):
                return true;
            default:
                return false;
        }
    }

    /**
     * @inheritdoc
     */
    protected function toArray()
    {
        $data = parent::toArray();
        $data = array_merge($data, $this->patternData);
        unset($data['patternData']);
        return $data;
    }
}
